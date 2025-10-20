package bg.tuvarna.devicebackend;

import bg.tuvarna.devicebackend.controllers.UserController;
import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.ChangePasswordVO;
import bg.tuvarna.devicebackend.models.dtos.UserCreateVO;
import bg.tuvarna.devicebackend.models.dtos.UserListing;
import bg.tuvarna.devicebackend.models.dtos.UserUpdateVO;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import bg.tuvarna.devicebackend.services.UserService;
import bg.tuvarna.devicebackend.utils.CustomPage;
import bg.tuvarna.devicebackend.config.JwtService;
import bg.tuvarna.devicebackend.config.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registration_shouldReturnOk_whenValidDataProvided() throws Exception {
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

        doNothing().when(userService).register(any(UserCreateVO.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateVO)))
                .andExpect(status().isOk());

        verify(userService, times(1)).register(any(UserCreateVO.class));
    }

    @Test
    void registration_shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
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

        doThrow(new CustomException("Email already taken", ErrorCode.AlreadyExists))
                .when(userService).register(any(UserCreateVO.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateVO)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).register(any(UserCreateVO.class));
    }

    @Test
    void registration_shouldReturnBadRequest_whenPhoneAlreadyExists() throws Exception {
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

        doThrow(new CustomException("Phone already taken", ErrorCode.AlreadyExists))
                .when(userService).register(any(UserCreateVO.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateVO)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).register(any(UserCreateVO.class));
    }

    @Test
    void registration_shouldReturnBadRequest_whenInvalidEmail() throws Exception {
        // Arrange
        UserCreateVO userCreateVO = new UserCreateVO(
                "John Doe",
                "Password123!",
                "invalid-email",
                "1234567890",
                "123 Main Street",
                LocalDate.now(),
                "DEVICE123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/registration")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userCreateVO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(UserCreateVO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_shouldReturnUserList_whenNoSearchProvided() throws Exception {
        // Arrange
        CustomPage<UserListing> customPage = new CustomPage<>();
        customPage.setCurrentPage(1);
        customPage.setTotalPages(1);
        customPage.setSize(10);
        customPage.setTotalItems(2L);

        List<UserListing> users = new ArrayList<>();
        User user1 = new User();
        user1.setId(1L);
        user1.setFullName("John Doe");
        user1.setEmail("john@example.com");
        user1.setPhone("1234567890");
        user1.setAddress("123 Main St");
        user1.setRole(UserRole.USER);
        users.add(new UserListing(user1));

        User user2 = new User();
        user2.setId(2L);
        user2.setFullName("Jane Smith");
        user2.setEmail("jane@example.com");
        user2.setPhone("0987654321");
        user2.setAddress("456 Oak Ave");
        user2.setRole(UserRole.USER);
        users.add(new UserListing(user2));

        customPage.setItems(users);

        when(userService.getUsers(null, 1, 10)).thenReturn(customPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .with(csrf())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$.items[1].fullName").value("Jane Smith"));

        verify(userService, times(1)).getUsers(null, 1, 10);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_shouldReturnFilteredUsers_whenSearchProvided() throws Exception {
        // Arrange
        CustomPage<UserListing> customPage = new CustomPage<>();
        customPage.setCurrentPage(1);
        customPage.setTotalPages(1);
        customPage.setSize(10);
        customPage.setTotalItems(1L);

        List<UserListing> users = new ArrayList<>();
        User user = new User();
        user.setId(1L);
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setPhone("1234567890");
        user.setAddress("123 Main St");
        user.setRole(UserRole.USER);
        users.add(new UserListing(user));

        customPage.setItems(users);

        when(userService.getUsers("john", 1, 10)).thenReturn(customPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .with(csrf())
                        .param("searchBy", "john")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("John Doe"));

        verify(userService, times(1)).getUsers("john", 1, 10);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_shouldReturnUpdatedUser_whenValidDataProvided() throws Exception {
        // Arrange
        Long userId = 1L;
        UserUpdateVO updateVO = new UserUpdateVO(
                "Updated Name",
                "Updated Address",
                "9999999999",
                "updated@example.com"
        );

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setFullName("Updated Name");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setPhone("9999999999");
        updatedUser.setAddress("Updated Address");
        updatedUser.setRole(UserRole.USER);
        updatedUser.setDevices(new ArrayList<>()); // Initialize devices list

        when(userService.updateUser(eq(userId), any(UserUpdateVO.class))).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.phone").value("9999999999"))
                .andExpect(jsonPath("$.address").value("Updated Address"));

        verify(userService, times(1)).updateUser(eq(userId), any(UserUpdateVO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        // Arrange
        Long userId = 999L;
        UserUpdateVO updateVO = new UserUpdateVO(
                "Updated Name",
                "Updated Address",
                "9999999999",
                "updated@example.com"
        );

        when(userService.updateUser(eq(userId), any(UserUpdateVO.class)))
                .thenThrow(new CustomException("User not found", ErrorCode.EntityNotFound));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVO)))
                .andExpect(status().isBadRequest()) // Your exception handler returns 400 for all CustomExceptions
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(userService, times(1)).updateUser(eq(userId), any(UserUpdateVO.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_shouldReturnBadRequest_whenEmailAlreadyTaken() throws Exception {
        // Arrange
        Long userId = 1L;
        UserUpdateVO updateVO = new UserUpdateVO(
                "Updated Name",
                "Updated Address",
                "9999999999",
                "taken@example.com"
        );

        when(userService.updateUser(eq(userId), any(UserUpdateVO.class)))
                .thenThrow(new CustomException("Email already taken", ErrorCode.AlreadyExists));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVO)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).updateUser(eq(userId), any(UserUpdateVO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void changePassword_shouldReturnOk_whenValidPasswordProvided() throws Exception {
        // Arrange
        Long userId = 1L;
        ChangePasswordVO changePasswordVO = new ChangePasswordVO("oldPassword", "newPassword123!");

        doNothing().when(userService).updatePassword(eq(userId), any(ChangePasswordVO.class));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}/changePassword", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordVO)))
                .andExpect(status().isOk());

        verify(userService, times(1)).updatePassword(eq(userId), any(ChangePasswordVO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void changePassword_shouldReturnBadRequest_whenOldPasswordDoesNotMatch() throws Exception {
        // Arrange
        Long userId = 1L;
        ChangePasswordVO changePasswordVO = new ChangePasswordVO("wrongPassword", "newPassword123!");

        doThrow(new CustomException("Old password didn't match", ErrorCode.Validation))
                .when(userService).updatePassword(eq(userId), any(ChangePasswordVO.class));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}/changePassword", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordVO)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).updatePassword(eq(userId), any(ChangePasswordVO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void changePassword_shouldReturnBadRequest_whenUserIsAdmin() throws Exception {
        // Arrange
        Long userId = 1L;
        ChangePasswordVO changePasswordVO = new ChangePasswordVO("oldPassword", "newPassword123!");

        doThrow(new CustomException("Admin password can't be changed", ErrorCode.Validation))
                .when(userService).updatePassword(eq(userId), any(ChangePasswordVO.class));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}/changePassword", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordVO)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).updatePassword(eq(userId), any(ChangePasswordVO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userLogin_shouldReturnOk_whenValidCredentialsProvided() throws Exception {
        // Arrange
        String loginJson = objectMapper.writeValueAsString(
            new bg.tuvarna.devicebackend.models.dtos.UserLoginDTO("testuser", "testpassword")
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_shouldReturnEmptyList_whenNoUsersExist() throws Exception {
        // Arrange
        CustomPage<UserListing> customPage = new CustomPage<>();
        customPage.setCurrentPage(1);
        customPage.setTotalPages(0);
        customPage.setSize(10);
        customPage.setTotalItems(0L);
        customPage.setItems(new ArrayList<>());

        when(userService.getUsers(null, 1, 10)).thenReturn(customPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .with(csrf())
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        verify(userService, times(1)).getUsers(null, 1, 10);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_shouldHandleDifferentPageSizes() throws Exception {
        // Arrange
        CustomPage<UserListing> customPage = new CustomPage<>();
        customPage.setCurrentPage(2);
        customPage.setTotalPages(3);
        customPage.setSize(5);
        customPage.setTotalItems(15L);
        customPage.setItems(new ArrayList<>());

        when(userService.getUsers(null, 2, 5)).thenReturn(customPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .with(csrf())
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(userService, times(1)).getUsers(null, 2, 5);
    }
}