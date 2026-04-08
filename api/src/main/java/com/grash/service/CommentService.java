package com.grash.service;

import com.grash.dto.comment.CommentCriteria;
import com.grash.dto.comment.CommentPatchDTO;
import com.grash.dto.comment.CommentPostDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.CommentMapper;
import com.grash.model.Comment;
import com.grash.model.Comment_;
import com.grash.model.User;
import com.grash.repository.CommentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final WorkOrderService workOrderService;
    private final EntityManager em;

    public Comment create(@Valid CommentPostDTO commentReq, User user) {
        Comment comment =
                commentMapper.fromPostDto(commentReq);
        workOrderService.checkAccessToWorkOrderId(commentReq.getWorkOrder().getId(), user);
        comment.setUser(user);
        Comment savedComment = commentRepository.saveAndFlush(comment);
        em.refresh(savedComment);
        return savedComment;
    }


    public List<Comment> getAll() {
        return commentRepository.findAll();
    }

    public void delete(Long id) {
        commentRepository.deleteById(id);
    }

    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    public Comment update(Long id, CommentPatchDTO commentPatchDTO, User user) {
        Comment savedComment =
                commentRepository.findById(id).orElseThrow(() -> new CustomException("Not found",
                        HttpStatus.NOT_FOUND));
        if (!savedComment.getUser().getId().equals(user.getId()))
            throw new CustomException("Access denied", HttpStatus.FORBIDDEN);

        return commentRepository.save(commentMapper.updateComment(savedComment, commentPatchDTO));
    }

    public List<Comment> findByCriteria(CommentCriteria criteria, User user) {
        Specification<Comment> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get(Comment_.company).get("id"), user.getCompany().getId()));
            predicates.add(cb.equal(root.get(Comment_.workOrder).get("id"), criteria.getWorkOrderId()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return commentRepository.findAll(specification);
    }

    public long countByWorkOrderId(Long workOrderId, User user) {
        workOrderService.checkAccessToWorkOrderId(workOrderId, user);
        return commentRepository.countByWorkOrderId(workOrderId);
    }
}
