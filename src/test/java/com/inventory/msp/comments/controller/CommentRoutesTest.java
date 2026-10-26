package com.inventory.msp.comments.controller;

import com.inventory.msp.notification.controller.InAppNotificationController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CommentRoutesTest {
    @Test
    void commentAndNotificationControllersExposeNoUpdateOrDeleteMappings() {
        List<Class<?>> controllers = List.of(
                CommentController.class,
                MentionableUserController.class,
                InAppNotificationController.class);

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                assertFalse(method.isAnnotationPresent(PutMapping.class), controller.getSimpleName());
                assertFalse(method.isAnnotationPresent(PatchMapping.class), controller.getSimpleName());
                assertFalse(method.isAnnotationPresent(DeleteMapping.class), controller.getSimpleName());
            }
        }
    }
}
