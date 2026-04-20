package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Data
@NoArgsConstructor
public class Comment extends CompanyAudit {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\[.*?\\]\\(user:(\\d+)\\)");

    @ManyToOne
    @NotNull
    private WorkOrder workOrder;

    @ManyToOne
    @NotNull
    private User user;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToMany
    private List<File> files = new ArrayList<>();

    public Set<Long> extractTaggedUserIds() {
        Set<Long> ids = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            ids.add(Long.parseLong(matcher.group(1)));
        }
        return ids;
    }
}