package bg.tuvarna.devicebackend;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.ChangePasswordVO;
import bg.tuvarna.devicebackend.models.dtos.UserCreateVO;
import bg.tuvarna.devicebackend.models.dtos.UserUpdateVO;
import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import bg.tuvarna.devicebackend.repositories.UserRepository;
import bg.tuvarna.devicebackend.services.DeviceService;
import bg.tuvarna.devicebackend.services.UserService;
import bg.tuvarna.devicebackend.utils.CustomPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private UserService userService;

    @Test
    void register_shouldSuccessfullyRegisterUser_whenValidDataProvided() {
        // Arrange
        UserCreateVO userCreateVO = new UserCreateVO(
                "John Doe",
                "Password123!",
                "john.doe@example.com",
                "1234567890",
                "123 Main Street",
                LocalDate.now(),
                "DEVICE123"
        );

        User savedUser = new User(userCreateVO);
        savedUser.setId(1L);
        savedUser.setPassword("encodedPassword");

        when(userRepository.getByEmail(anyString())).thenReturn(null);
        when(userRepository.getByPhone(anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        doNothing().when(deviceService).alreadyExist(anyString());
        when(deviceService.registerDevice(anyString(), any(LocalDate.class), any(User.class)))
                .thenReturn(new Device());

        // Act
        userService.register(userCreateVO);

        // Assert
        verify(userRepository).saveAndFlush(any(User.class));
        verify(deviceService).registerDevice("DEVICE123", userCreateVO.purchaseDate(), savedUser);
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyTaken() {
        // Arrange
        UserCreateVO userCreateVO = new UserCreateVO(
                "John Doe",
                "Password123!",
                "existing@example.com",
                "1234567890",
                "123 Main Street",
                LocalDate.now(),
                "DEVICE123"
        );

        User existingUser = new User();
        when(userRepository.getByEmail("existing@example.com")).thenReturn(existingUser);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.register(userCreateVO);
        });

        assertEquals("Email already taken", exception.getMessage());
        assertEquals(ErrorCode.AlreadyExists, exception.getErrorCode());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenPhoneAlreadyTaken() {
        // Arrange
        UserCreateVO userCreateVO = new UserCreateVO(
                "John Doe",
                "Password123!",
                "john@example.com",
                "1234567890",
                "123 Main Street",
                LocalDate.now(),
                "DEVICE123"
        );

        User existingUser = new User();
        when(userRepository.getByEmail(anyString())).thenReturn(null);
        when(userRepository.getByPhone("1234567890")).thenReturn(existingUser);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.register(userCreateVO);
        });

        assertEquals("Phone already taken", exception.getMessage());
        assertEquals(ErrorCode.AlreadyExists, exception.getErrorCode());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void register_shouldRollbackUser_whenDeviceRegistrationFails() {
        // Arrange
        UserCreateVO userCreateVO = new UserCreateVO(
                "John Doe",
                "Password123!",
                "john@example.com",
                "1234567890",
                "123 Main Street",
                LocalDate.now(),
                "INVALID_DEVICE"
        );

        User savedUser = new User(userCreateVO);
        savedUser.setId(1L);

        when(userRepository.getByEmail(anyString())).thenReturn(null);
        when(userRepository.getByPhone(anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        doThrow(new CustomException("Device already registered", ErrorCode.AlreadyExists))
                .when(deviceService).alreadyExist("INVALID_DEVICE");

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            userService.register(userCreateVO);
        });

        verify(userRepository).delete(savedUser);
    }

    @Test
    void register_shouldSucceed_whenDeviceSerialNumberIsNull() {
        // Arrange
        UserCreateVO userCreateVO = new UserCreateVO(
                "John Doe",
                "Password123!",
                "john@example.com",
                "1234567890",
                "123 Main Street",
                null,
                null
        );

        when(userRepository.getByEmail(anyString())).thenReturn(null);
        when(userRepository.getByPhone(anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.register(userCreateVO);

        // Assert
        verify(userRepository).saveAndFlush(any(User.class));
        verify(deviceService, never()).registerDevice(anyString(), any(), any());
    }

    @Test
    void register_shouldSucceed_whenDeviceSerialNumberIsBlank() {
        // Arrange
        UserCreateVO userCreateVO = new UserCreateVO(
                "John Doe",
                "Password123!",
                "john.doe@example.com",
                "1234567890",
                "123 Main Street",
                LocalDate.now(),
                "   "
        );

        when(userRepository.getByEmail(anyString())).thenReturn(null);
        when(userRepository.getByPhone(anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.register(userCreateVO);

        // Assert
        verify(userRepository).saveAndFlush(any(User.class));
        verify(deviceService, never()).registerDevice(anyString(), any(), any());
    }

    @Test
    void isEmailTaken_shouldReturnTrue_whenEmailExists() {
        // Arrange
        when(userRepository.getByEmail("existing@example.com")).thenReturn(new User());

        // Act
        boolean result = userService.isEmailTaken("existing@example.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void isEmailTaken_shouldReturnFalse_whenEmailDoesNotExist() {
        // Arrange
        when(userRepository.getByEmail("new@example.com")).thenReturn(null);

        // Act
        boolean result = userService.isEmailTaken("new@example.com");

        // Assert
        assertFalse(result);
    }

    @Test
    void isPhoneTaken_shouldReturnTrue_whenPhoneExists() {
        // Arrange
        when(userRepository.getByPhone("1234567890")).thenReturn(new User());

        // Act
        boolean result = userService.isPhoneTaken("1234567890");

        // Assert
        assertTrue(result);
    }

    @Test
    void isPhoneTaken_shouldReturnFalse_whenPhoneDoesNotExist() {
        // Arrange
        when(userRepository.getByPhone("9999999999")).thenReturn(null);

        // Act
        boolean result = userService.isPhoneTaken("9999999999");

        // Assert
        assertFalse(result);
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        User result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserById_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.getUserById(999L);
        });

        assertEquals("User not found", exception.getMessage());
        assertEquals(ErrorCode.EntityNotFound, exception.getErrorCode());
    }

    @Test
    void getUserByUsername_shouldReturnUser_whenUserExists() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");

        when(userRepository.findByEmailOrPhone("test@example.com")).thenReturn(Optional.of(user));

        // Act
        User result = userService.getUserByUsername("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserByUsername_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.findByEmailOrPhone("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.getUserByUsername("nonexistent@example.com");
        });

        assertEquals("User not found", exception.getMessage());
        assertEquals(ErrorCode.EntityNotFound, exception.getErrorCode());
    }

    @Test
    void updatePassword_shouldUpdatePassword_whenOldPasswordMatches() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setPassword("oldEncodedPassword");
        user.setRole(UserRole.USER);

        ChangePasswordVO changePasswordVO = new ChangePasswordVO("oldPassword", "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "oldEncodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.updatePassword(1L, changePasswordVO);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("newEncodedPassword", userCaptor.getValue().getPassword());
    }

    @Test
    void updatePassword_shouldThrowException_whenOldPasswordDoesNotMatch() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setPassword("encodedPassword");
        user.setRole(UserRole.USER);

        ChangePasswordVO changePasswordVO = new ChangePasswordVO("wrongPassword", "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.updatePassword(1L, changePasswordVO);
        });

        assertEquals("Old password didn't match", exception.getMessage());
        assertEquals(ErrorCode.Validation, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updatePassword_shouldThrowException_whenUserIsAdmin() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setRole(UserRole.ADMIN);

        ChangePasswordVO changePasswordVO = new ChangePasswordVO("oldPassword", "newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.updatePassword(1L, changePasswordVO);
        });

        assertEquals("Admin password can't be changed", exception.getMessage());
        assertEquals(ErrorCode.Validation, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_shouldUpdateUser_whenValidDataProvided() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");
        existingUser.setPhone("0000000000");
        existingUser.setRole(UserRole.USER);

        UserUpdateVO updateVO = new UserUpdateVO(
                "Updated Name",
                "New Address",
                "1111111111",
                "new@example.com"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.getByEmail("new@example.com")).thenReturn(null);
        when(userRepository.getByPhone("1111111111")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        userService.updateUser(1L, updateVO);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("Updated Name", capturedUser.getFullName());
        assertEquals("new@example.com", capturedUser.getEmail());
        assertEquals("1111111111", capturedUser.getPhone());
        assertEquals("New Address", capturedUser.getAddress());
    }

    @Test
    void updateUser_shouldThrowException_whenUserIsAdmin() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setRole(UserRole.ADMIN);

        UserUpdateVO updateVO = new UserUpdateVO(
                "New Name",
                "Address",
                "1234567890",
                "admin@example.com"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.updateUser(1L, updateVO);
        });

        assertEquals("Admin password can't be changed", exception.getMessage());
        assertEquals(ErrorCode.Validation, exception.getErrorCode());
    }

    @Test
    void updateUser_shouldThrowException_whenNewEmailAlreadyTaken() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");
        existingUser.setRole(UserRole.USER);

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("taken@example.com");

        UserUpdateVO updateVO = new UserUpdateVO(
                "Name",
                "Address",
                "1234567890",
                "taken@example.com"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.getByEmail("taken@example.com")).thenReturn(anotherUser);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.updateUser(1L, updateVO);
        });

        assertEquals("Email already taken", exception.getMessage());
        assertEquals(ErrorCode.AlreadyExists, exception.getErrorCode());
    }

    @Test
    void updateUser_shouldThrowException_whenNewPhoneAlreadyTaken() {
        // Arrange
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPhone("0000000000");
        existingUser.setEmail("user@example.com");
        existingUser.setRole(UserRole.USER);

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setPhone("1234567890");

        UserUpdateVO updateVO = new UserUpdateVO(
                "Name",
                "Address",
                "1234567890",
                "user@example.com"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.getByEmail("user@example.com")).thenReturn(existingUser);
        when(userRepository.getByPhone("1234567890")).thenReturn(anotherUser);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.updateUser(1L, updateVO);
        });

        assertEquals("Phone already taken", exception.getMessage());
        assertEquals(ErrorCode.AlreadyExists, exception.getErrorCode());
    }

    @Test
    void getUsers_shouldReturnAllUsers_whenSearchByIsNull() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@example.com");
        user1.setDevices(new ArrayList<>());

        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@example.com");
        user2.setDevices(new ArrayList<>());

        List<User> users = Arrays.asList(user1, user2);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 2);

        when(userRepository.getAllUsers(PageRequest.of(0, 10))).thenReturn(userPage);

        // Act
        CustomPage result = userService.getUsers(null, 1, 10);

        // Assert
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getCurrentPage());
        assertEquals(10, result.getSize());
        assertEquals(2, result.getTotalItems());
        assertEquals(2, result.getItems().size());
    }

    @Test
    void getUsers_shouldReturnFilteredUsers_whenSearchByProvided() {
        // Arrange
        Device device1 = new Device();
        device1.setSerialNumber("ABC123");

        Device device2 = new Device();
        device2.setSerialNumber("XYZ789");

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setDevices(new ArrayList<>(Arrays.asList(device1, device2)));

        List<User> users = Arrays.asList(user);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 1);

        when(userRepository.searchBy("ABC", PageRequest.of(0, 10))).thenReturn(userPage);

        // Act
        CustomPage result = userService.getUsers("ABC", 1, 10);

        // Assert
        assertEquals(1, result.getTotalItems());
        verify(userRepository).searchBy("ABC", PageRequest.of(0, 10));
    }

    @Test
    void getUsers_shouldHandlePagination() {
        // Arrange
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setId((long) i);
            user.setDevices(new ArrayList<>());
            users.add(user);
        }

        Page<User> userPage = new PageImpl<>(users, PageRequest.of(1, 5), 15);

        when(userRepository.getAllUsers(PageRequest.of(1, 5))).thenReturn(userPage);

        // Act
        CustomPage result = userService.getUsers(null, 2, 5);

        // Assert
        assertEquals(3, result.getTotalPages());
        assertEquals(2, result.getCurrentPage());
        assertEquals(5, result.getSize());
        assertEquals(15, result.getTotalItems());
    }
}
