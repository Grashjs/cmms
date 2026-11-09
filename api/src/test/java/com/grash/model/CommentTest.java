package com.grash.model;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommentTest {
    @Test void overflowingAndMalformedMentionsCannotHideValidRecipients() {
        String content = "@[First](user:34) @[Overflow](user:999999999999999999999999999999) "
                + "@[Malformed](user:abc) @[Last](user:35) @[First again](user:34)";
        Comment comment = new Comment(); comment.setContent(content);
        assertEquals(Set.of(34L, 35L), comment.extractTaggedUserIds());
        assertEquals(content, comment.getContent());
    }
}
