package com.georgi.shakev.OnlineVideoLearningPlatform.controller;

import com.georgi.shakev.OnlineVideoLearningPlatform.dto.LessonRequestDto;
import com.georgi.shakev.OnlineVideoLearningPlatform.dto.LessonResponseDto;
import com.georgi.shakev.OnlineVideoLearningPlatform.dto.ReviewRequestDto;
import com.georgi.shakev.OnlineVideoLearningPlatform.exception.ResourceNotFoundException;
import com.georgi.shakev.OnlineVideoLearningPlatform.service.LessonService;
import com.georgi.shakev.OnlineVideoLearningPlatform.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
@Slf4j
public class LessonController {

    private final LessonService lessonService;
    private final ReviewService reviewService;

    @Autowired
    public LessonController(LessonService lessonService, ReviewService reviewService) {
        this.lessonService = lessonService;
        this.reviewService = reviewService;
    }

    @ModelAttribute("lessonRequest")
    public LessonRequestDto lessonRequestDto() {
        return new LessonRequestDto();
    }

    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @PostMapping("/home")
    public String postLesson(LessonRequestDto lessonRequestDto, @AuthenticationPrincipal User principal) throws IOException {
        LessonResponseDto response = lessonService.createLesson(lessonRequestDto, principal.getUsername());
        log.info("Lesson {} created.", response);
        return "redirect:/home/" + response.getId();
    }

    @GetMapping("/home/{lessonId}")
    public String getLesson(Model model, @PathVariable Long lessonId,
    HttpServletRequest request, @AuthenticationPrincipal User principal){
        int reviewPage = 0;
        if (request.getParameter("reviewPage") != null && !request.getParameter("reviewPage").isEmpty()) {
            reviewPage = Integer.parseInt(request.getParameter("reviewPage"));
        }
        int allPages = reviewService.getReviews(reviewPage, lessonId).getTotalPages();
        if(reviewPage < 0 || reviewPage > allPages){
            throw new ResourceNotFoundException("Invalid page number.");
        }
        model.addAttribute("username", principal.getUsername());
        model.addAttribute("reviewRequest", new ReviewRequestDto());
        model.addAttribute("lesson", lessonService.getLesson(lessonId));
        model.addAttribute("reviews", reviewService.getReviews(reviewPage, lessonId).getContent());
        model.addAttribute("reviewPage", reviewPage + 1);
        model.addAttribute("allPagesNumber", allPages);
        log.info("Lesson with id {} accessed by {}", lessonId, principal.getUsername());
        return "lesson";
    }

    @PreAuthorize("hasRole('MODERATOR') or hasRole('ADMIN')")
    @GetMapping("/home/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long lessonId, Model model) {
        lessonService.deleteLesson(lessonId);
        log.info("Lesson with id {} deleted", lessonId);
        return "redirect:/home?deleted";
    }

    @GetMapping("/home/{lessonId}/view-video")
    public ResponseEntity<?> getVideo(@PathVariable Long lessonId){
        log.info("Video of lesson with id {} accessed", lessonId);
        return lessonService.viewVideo(lessonId);
    }

    @PostMapping("/home/{lessonId}/upload-video")
    public String uploadVideo(@PathVariable Long lessonId,
                                       @RequestParam("file") MultipartFile file) throws IOException {
        if(!file.isEmpty()) {
            lessonService.uploadVideo(lessonId, file);
        }
        log.info("Video added to lesson with id {}", lessonId);
        return "redirect:/home/" + lessonId;
    }

    @GetMapping("/home/{lessonId}/remove-video")
    public String removeVideo(@PathVariable Long lessonId, Model model){
        lessonService.removeVideo(lessonId);
        log.info("Video removed from lesson with id {}", lessonId);
        return "redirect:/home/" + lessonId;
    }
}
