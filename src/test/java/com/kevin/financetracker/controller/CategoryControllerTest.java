package com.kevin.financetracker.controller;

import com.kevin.financetracker.dto.CategoryRequestDTO;
import com.kevin.financetracker.exception.GlobalExceptionHandler;
import com.kevin.financetracker.exception.ResourceNotFoundException;
import com.kevin.financetracker.model.Category;
import com.kevin.financetracker.dto.CategoryDTO;
import com.kevin.financetracker.model.CategoryType;
import com.kevin.financetracker.model.User;
import com.kevin.financetracker.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private ObjectMapper objectMapper;
    private User user;
    private User differentUser;
    private Category parentCategory;
    private Category childCategory;
    private Category incomeCategory;
    private Category groceriesCategory;
    private CategoryDTO parentCategoryDTO;
    private CategoryDTO childCategoryDTO;
    private CategoryDTO incomeCategoryDTO;
    private CategoryDTO groceriesCategoryDTO;

    // Test constants
    private final Long userId = 1L;
    private final Long differentUserId = 2L;
    private final Long categoryId = 1L;
    private final Long parentCategoryId = 1L;
    private final Long nonExistentId = 999L;

    // Custom any() methods to resolve ambiguous method calls
    private static Category anyCategory() {
        return org.mockito.ArgumentMatchers.any(Category.class);
    }

    private static Long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }

    private static CategoryType anyCategoryType() {
        return org.mockito.ArgumentMatchers.any(CategoryType.class);
    }

    private static CategoryRequestDTO anyCategoryRequestDTO() {
        return org.mockito.ArgumentMatchers.any(CategoryRequestDTO.class);
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        // Setup main user
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@gmail.com");

        // Setup different user for security tests
        differentUser = new User();
        differentUser.setId(differentUserId);
        differentUser.setUsername("differentuser");
        differentUser.setEmail("different@gmail.com");

        // Parent category (top-level)
        parentCategory = new Category("Food", CategoryType.EXPENSE, user);
        parentCategory.setId(categoryId);
        parentCategory.setDescription("Food category");

        // Parent category DTO
        parentCategoryDTO = new CategoryDTO(categoryId, "Food", CategoryType.EXPENSE, "Food category", null, new ArrayList<>());

        // Child category (subcategory)
        childCategory = new Category("Fruits", CategoryType.EXPENSE, user);
        childCategory.setId(2L);
        childCategory.setParentCategory(parentCategory);
        childCategory.setDescription("Fruits subcategory");

        // Child category DTO
        childCategoryDTO = new CategoryDTO(2L, "Fruits", CategoryType.EXPENSE, "Fruits subcategory", parentCategoryId, new ArrayList<>());

        // Income category
        incomeCategory = new Category("Salary", CategoryType.INCOME, user);
        incomeCategory.setId(3L);
        incomeCategory.setDescription("Income category");

        // Income category DTO
        incomeCategoryDTO = new CategoryDTO(3L, "Salary", CategoryType.INCOME, "Income category", null, new ArrayList<>());

        // Groceries category
        groceriesCategory = new Category("Groceries", CategoryType.EXPENSE, user);
        groceriesCategory.setId(4L);
        groceriesCategory.setDescription("Food and household items");

        // Groceries category DTO
        groceriesCategoryDTO = new CategoryDTO(4L, "Groceries", CategoryType.EXPENSE, "Food and household items", null, new ArrayList<>());
    }

    // =========================================
    // UNIT TESTS
    // =========================================

    @Test
    void createCategory_ShouldReturnCreatedCategory() {
        // Arrange
        // Use anyCategory() matcher instead of specific object to avoid strict matching
        when(categoryService.createCategory(eq(userId), anyCategory())).thenReturn(parentCategoryDTO);

        // Act
        ResponseEntity<CategoryDTO> response = categoryController.createCategory(userId, parentCategory);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(parentCategoryDTO, response.getBody());
        verify(categoryService, times(1)).createCategory(eq(userId), anyCategory());
    }

    @Test
    void createSubcategory_WhenParentNotFound_ShouldThrowException() {
        // Arrange
        when(categoryService.getCategoryById(parentCategoryId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryController.createSubcategory(userId, parentCategoryId, childCategory));
        assertEquals("Parent category not found with id: " + parentCategoryId, exception.getMessage());
        verify(categoryService, times(1)).getCategoryById(parentCategoryId);
        verify(categoryService, never()).createSubcategory(anyLong(), anyCategory());
    }

    @Test
    void createSubcategory_WhenWrongUser_ShouldThrowException() {
        // Arrange
        Category foreignCategory = new Category("Foreign", CategoryType.EXPENSE, differentUser);
        when(categoryService.getCategoryById(parentCategoryId)).thenReturn(Optional.of(foreignCategory));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryController.createSubcategory(userId, parentCategoryId, childCategory));
        assertEquals("Category not found for this user", exception.getMessage());
        verify(categoryService, times(1)).getCategoryById(parentCategoryId);
        verify(categoryService, never()).createSubcategory(anyLong(), anyCategory());
    }

    @Test
    void getCategoriesByUser_ShouldReturnCategoriesList() {
        // Arrange
        List<CategoryDTO> categories = Arrays.asList(parentCategoryDTO, childCategoryDTO);
        when(categoryService.getCategoriesByUser(userId)).thenReturn(categories);

        // Act
        ResponseEntity<List<CategoryDTO>> response = categoryController.getCategoriesByUser(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categories, response.getBody());
        verify(categoryService, times(1)).getCategoriesByUser(userId);
    }

    @Test
    void getCategoryById_ShouldReturnCategory() {
        // Arrange
        when(categoryService.getCategoryById(categoryId)).thenReturn(Optional.of(parentCategory));
        when(categoryService.convertToDTO(parentCategory)).thenReturn(parentCategoryDTO);

        // Act
        ResponseEntity<CategoryDTO> response = categoryController.getCategoryById(userId, categoryId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(parentCategoryDTO, response.getBody());
        verify(categoryService, times(1)).getCategoryById(categoryId);
        verify(categoryService, times(1)).convertToDTO(parentCategory);
    }

    @Test
    void getCategoryById_WhenNotFound_ShouldThrowException() {
        // Arrange
        when(categoryService.getCategoryById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryController.getCategoryById(userId, nonExistentId));
        assertEquals("Category not found with id: " + nonExistentId, exception.getMessage());
        verify(categoryService, times(1)).getCategoryById(nonExistentId);
    }

    @Test
    void getCategoryById_WhenWrongUser_ShouldThrowException() {
        // Arrange
        Category foreignCategory = new Category("Foreign", CategoryType.EXPENSE, differentUser);
        when(categoryService.getCategoryById(categoryId)).thenReturn(Optional.of(foreignCategory));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryController.getCategoryById(userId, categoryId));
        assertEquals("Category not found for this user", exception.getMessage());
        verify(categoryService, times(1)).getCategoryById(categoryId);
    }

    @Test
    void getCategoriesByType_ShouldReturnFilteredCategories() {
        // Arrange
        List<CategoryDTO> categories = Arrays.asList(parentCategoryDTO);
        when(categoryService.getCategoriesByUserAndType(userId, CategoryType.EXPENSE)).thenReturn(categories);

        // Act
        ResponseEntity<List<CategoryDTO>> response = categoryController.getCategoriesByType(userId, CategoryType.EXPENSE);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categories, response.getBody());
        verify(categoryService, times(1)).getCategoriesByUserAndType(userId, CategoryType.EXPENSE);
    }

    @Test
    void getTopLevelCategories_ShouldReturnTopLevelCategories() {
        // Arrange
        List<CategoryDTO> categories = Arrays.asList(parentCategoryDTO);
        when(categoryService.getTopLevelCategoriesByUser(userId)).thenReturn(categories);

        // Act
        ResponseEntity<List<CategoryDTO>> response = categoryController.getTopLevelCategories(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categories, response.getBody());
        verify(categoryService, times(1)).getTopLevelCategoriesByUser(userId);
    }

    @Test
    void getSubcategories_ShouldReturnSubcategories() {
        // Arrange
        List<CategoryDTO> subcategories = Arrays.asList(childCategoryDTO);
        when(categoryService.getCategoryById(parentCategoryId)).thenReturn(Optional.of(parentCategory));
        when(categoryService.getSubcategories(parentCategoryId)).thenReturn(subcategories);

        // Act
        ResponseEntity<List<CategoryDTO>> response = categoryController.getSubcategories(userId, parentCategoryId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(subcategories, response.getBody());
        verify(categoryService, times(1)).getCategoryById(parentCategoryId);
        verify(categoryService, times(1)).getSubcategories(parentCategoryId);
    }

    @Test
    void updateCategory_WhenNotFound_ShouldThrowException() {
        // Arrange
        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Updated Food");
        requestDTO.setType(CategoryType.EXPENSE);

        when(categoryService.getCategoryById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> categoryController.updateCategory(userId, nonExistentId, requestDTO));
        assertEquals("Category not found with id: " + nonExistentId, exception.getMessage());
        verify(categoryService, times(1)).getCategoryById(nonExistentId);
        verify(categoryService, never()).updateCategory(anyLong(), anyCategory());
    }

    @Test
    void deleteCategory_ShouldReturnSuccessMessage() {
        // Arrange
        when(categoryService.getCategoryById(categoryId)).thenReturn(Optional.of(parentCategory));
        doNothing().when(categoryService).deleteCategory(categoryId);

        // Act
        ResponseEntity<String> response = categoryController.deleteCategory(userId, categoryId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Category deleted successfully", response.getBody());
        verify(categoryService, times(1)).getCategoryById(categoryId);
        verify(categoryService, times(1)).deleteCategory(categoryId);
    }

    @Test
    void searchCategoriesByName_ShouldReturnMatchingCategories() {
        // Arrange
        String searchTerm = "Food";
        List<CategoryDTO> categories = Arrays.asList(parentCategoryDTO);
        when(categoryService.searchCategoriesByName(userId, searchTerm)).thenReturn(categories);

        // Act
        ResponseEntity<List<CategoryDTO>> response = categoryController.searchCategoriesByName(userId, searchTerm);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categories, response.getBody());
        verify(categoryService, times(1)).searchCategoriesByName(userId, searchTerm);
    }

    @Test
    void searchCategoriesByName_WhenTooShort_ShouldThrowException() {
        // Arrange
        String searchTerm = "F";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryController.searchCategoriesByName(userId, searchTerm));
        assertEquals("Search term must be at least 2 characters long", exception.getMessage());
        verify(categoryService, never()).searchCategoriesByName(anyLong(), anyString());
    }

    // =========================================
    // INTEGRATION TESTS (HTTP endpoints)
    // =========================================

    @Test
    void createCategory_HttpTest_ShouldReturnCreatedCategory() throws Exception {
        // Arrange
        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Utilities");
        requestDTO.setType(CategoryType.EXPENSE);
        requestDTO.setDescription("Utility bills");

        CategoryDTO responseDTO = new CategoryDTO(5L, "Utilities", CategoryType.EXPENSE, "Utility bills", null, Collections.emptyList());

        when(categoryService.createCategory(eq(userId), anyCategory())).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/users/{userId}/categories", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.name", is("Utilities")))
                .andExpect(jsonPath("$.type", is("EXPENSE")))
                .andExpect(jsonPath("$.description", is("Utility bills")));

        verify(categoryService).createCategory(eq(userId), anyCategory());
    }

    @Test
    void createSubcategory_HttpTest_ShouldReturnCreatedSubcategory() throws Exception {
        // Arrange
        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Vegetables");
        requestDTO.setType(CategoryType.EXPENSE);
        requestDTO.setDescription("Vegetables subcategory");

        CategoryDTO responseDTO = new CategoryDTO(5L, "Vegetables", CategoryType.EXPENSE, "Vegetables subcategory", parentCategoryId, Collections.emptyList());

        when(categoryService.getCategoryById(parentCategoryId)).thenReturn(Optional.of(parentCategory));
        when(categoryService.createSubcategory(eq(parentCategoryId), anyCategory())).thenReturn(responseDTO);

        // Act & Assert - Only perform once
        mockMvc.perform(post("/api/users/{userId}/categories/{parentCategoryId}/subcategories", userId, parentCategoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.name", is("Vegetables")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        verify(categoryService, times(1)).getCategoryById(parentCategoryId);
        verify(categoryService, times(1)).createSubcategory(eq(parentCategoryId), anyCategory());
    }

    @Test
    void getCategoriesByUser_HttpTest_ShouldReturnCategoriesList() throws Exception {
        // Arrange
        List<CategoryDTO> categories = Arrays.asList(parentCategoryDTO, incomeCategoryDTO, groceriesCategoryDTO);
        when(categoryService.getCategoriesByUser(userId)).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/categories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("Food")))
                .andExpect(jsonPath("$[1].name", is("Salary")))
                .andExpect(jsonPath("$[2].name", is("Groceries")));

        verify(categoryService).getCategoriesByUser(userId);
    }

    @Test
    void getCategoryById_HttpTest_ShouldReturnCategory() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(categoryId)).thenReturn(Optional.of(parentCategory));
        when(categoryService.convertToDTO(parentCategory)).thenReturn(parentCategoryDTO);

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/categories/{categoryId}", userId, categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Food")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        verify(categoryService).getCategoryById(categoryId);
        verify(categoryService).convertToDTO(parentCategory);
    }

    @Test
    void getCategoryById_HttpTest_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/categories/{categoryId}", userId, nonExistentId))
                .andExpect(status().isNotFound());

        verify(categoryService).getCategoryById(nonExistentId);
    }

    @Test
    void updateCategory_HttpTest_ShouldReturnUpdatedCategory() throws Exception {
        // Arrange
        CategoryRequestDTO requestDTO = new CategoryRequestDTO();
        requestDTO.setName("Updated Food");
        requestDTO.setDescription("Updated description");
        requestDTO.setType(CategoryType.EXPENSE);

        CategoryDTO responseDTO = new CategoryDTO(categoryId, "Updated Food", CategoryType.EXPENSE, "Updated description", null, Collections.emptyList());

        when(categoryService.getCategoryById(categoryId)).thenReturn(Optional.of(parentCategory));
        when(categoryService.updateCategory(eq(categoryId), anyCategory())).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(put("/api/users/{userId}/categories/{categoryId}", userId, categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Food")))
                .andExpect(jsonPath("$.description", is("Updated description")));

        verify(categoryService).getCategoryById(categoryId);
        verify(categoryService).updateCategory(eq(categoryId), anyCategory());
    }

    @Test
    void deleteCategory_HttpTest_ShouldReturnSuccessMessage() throws Exception {
        // Arrange
        when(categoryService.getCategoryById(categoryId)).thenReturn(Optional.of(parentCategory));
        doNothing().when(categoryService).deleteCategory(categoryId);

        // Act & Assert
        mockMvc.perform(delete("/api/users/{userId}/categories/{categoryId}", userId, categoryId))
                .andExpect(status().isOk())
                .andExpect(content().string("Category deleted successfully"));

        verify(categoryService).getCategoryById(categoryId);
        verify(categoryService).deleteCategory(categoryId);
    }

    @Test
    void searchCategoriesByName_HttpTest_ShouldReturnMatchingCategories() throws Exception {
        // Arrange
        List<CategoryDTO> matchingCategories = Arrays.asList(parentCategoryDTO, groceriesCategoryDTO);
        when(categoryService.searchCategoriesByName(userId, "food")).thenReturn(matchingCategories);

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/categories/search", userId)
                        .param("name", "food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Food")))
                .andExpect(jsonPath("$[1].name", is("Groceries")));

        verify(categoryService).searchCategoriesByName(userId, "food");
    }

    @Test
    void searchCategoriesByName_HttpTest_WhenTooShort_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/categories/search", userId)
                        .param("name", "a")) // Only 1 character
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).searchCategoriesByName(anyLong(), anyString());
    }
}