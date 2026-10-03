package com.jay.aicodemother.controller;

import com.jay.aicodemother.ai.AiCodeGenTypeRoutingService;
import com.jay.aicodemother.common.BaseResponse;
import com.jay.aicodemother.constant.AppConstant;
import com.jay.aicodemother.model.entity.App;
import com.jay.aicodemother.model.entity.User;
import com.jay.aicodemother.model.enums.CodeGenTypeEnum;
import com.jay.aicodemother.service.AppService;
import com.jay.aicodemother.service.ProjectDownloadService;
import com.jay.aicodemother.service.PreviewTokenService;
import com.jay.aicodemother.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppControllerPreviewTest {

    private static final long APP_ID = 991002L;
    private final Path projectRoot = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + APP_ID);
    private AppController controller;
    private AppService appService;
    private UserService userService;
    private App app;

    @BeforeEach
    void setUp() throws IOException {
        appService = mock(AppService.class);
        userService = mock(UserService.class);
        controller = new AppController(
                appService,
                userService,
                mock(ProjectDownloadService.class),
                mock(AiCodeGenTypeRoutingService.class),
                new PreviewTokenService()
        );
        app = App.builder()
                .id(APP_ID)
                .userId(10L)
                .codeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue())
                .build();
        User user = new User();
        user.setId(10L);
        when(appService.getById(APP_ID)).thenReturn(app);
        when(userService.getLoginUser(org.mockito.ArgumentMatchers.any())).thenReturn(user);

        Files.createDirectories(projectRoot.resolve("dist/assets"));
        Files.writeString(projectRoot.resolve("dist/index.html"), "<div id=\"app\"></div>");
        Files.writeString(projectRoot.resolve("dist/assets/index.js"), "console.log('ready')");
    }

    @Test
    void updatedAssetMakesPreviewFreshWhenEntryFileIsUnchanged() throws IOException {
        long generatedAfter = System.currentTimeMillis() - 1_000;
        Files.setLastModifiedTime(projectRoot.resolve("dist/index.html"),
                FileTime.fromMillis(generatedAfter - 5_000));
        Files.setLastModifiedTime(projectRoot.resolve("dist/assets/index.js"),
                FileTime.fromMillis(generatedAfter + 500));

        ResponseEntity<Resource> response = controller.preview(
                APP_ID,
                generatedAfter,
                previewRequest("/app/preview/" + APP_ID + "/")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @AfterEach
    void cleanUp() throws IOException {
        if (!Files.exists(projectRoot)) {
            return;
        }
        try (var paths = Files.walk(projectRoot)) {
            paths.sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> path.toFile().delete());
        }
    }

    @Test
    void tokenLoadsVueEntryAndAssetsWithoutForwardingLoginSession() {
        BaseResponse<String> tokenResponse = controller.createPreviewToken(APP_ID, new MockHttpServletRequest());
        String token = tokenResponse.getData();
        assertNotNull(token);

        org.mockito.Mockito.clearInvocations(userService);
        ResponseEntity<Resource> indexResponse = controller.preview(
                APP_ID,
                null,
                previewRequest("/app/preview/" + APP_ID + "/" + token + "/")
        );
        ResponseEntity<Resource> assetResponse = controller.preview(
                APP_ID,
                null,
                previewRequest("/app/preview/" + APP_ID + "/" + token + "/assets/index.js")
        );

        assertEquals(HttpStatus.OK, indexResponse.getStatusCode());
        assertEquals("index.html", indexResponse.getBody().getFilename());
        assertEquals("*", indexResponse.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals(HttpStatus.OK, assetResponse.getStatusCode());
        assertEquals("index.js", assetResponse.getBody().getFilename());
        assertEquals("*", assetResponse.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        verify(userService, never()).getLoginUser(org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest previewRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI("/api" + path);
        request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, path);
        return request;
    }
}
