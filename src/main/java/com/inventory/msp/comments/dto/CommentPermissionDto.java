package com.inventory.msp.comments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommentPermissionDto(@JsonProperty("can_comment") boolean canComment) {
}
