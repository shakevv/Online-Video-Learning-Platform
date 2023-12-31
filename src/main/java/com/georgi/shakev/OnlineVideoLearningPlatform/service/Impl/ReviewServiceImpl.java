package com.georgi.shakev.OnlineVideoLearningPlatform.service.Impl;

import com.georgi.shakev.OnlineVideoLearningPlatform.dto.ReviewRequestDto;
import com.georgi.shakev.OnlineVideoLearningPlatform.dto.ReviewResponseDto;
import com.georgi.shakev.OnlineVideoLearningPlatform.entity.Lesson;
import com.georgi.shakev.OnlineVideoLearningPlatform.entity.Review;
import com.georgi.shakev.OnlineVideoLearningPlatform.exception.ResourceNotFoundException;
import com.georgi.shakev.OnlineVideoLearningPlatform.repository.LessonRepository;
import com.georgi.shakev.OnlineVideoLearningPlatform.repository.ReviewRepository;
import com.georgi.shakev.OnlineVideoLearningPlatform.repository.UserRepository;
import com.georgi.shakev.OnlineVideoLearningPlatform.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final String sortBy = "id";
    private final int pageSize = 10;
    private final int defaultPageNumber = 0;

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             UserRepository userRepository,
                             LessonRepository lessonRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
    }

    @Override
    @Transactional
    public void createReview(ReviewRequestDto reviewRequest) {
       reviewRepository.save(dtoToEntity(reviewRequest));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        reviewRepository.delete(review);
        reviewRepository.findAll(PageRequest.of(defaultPageNumber, pageSize, Sort.by(sortBy).descending()))
                .map(this::entityToDto);
    }

    @Override
    public Page<ReviewResponseDto> getReviews(int page, Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found."));
        return reviewRepository.getAllByLessonId(
                lessonId, PageRequest.of(page, pageSize, Sort.by(sortBy).descending()))
                .map(this::entityToDto);
    }

    private ReviewResponseDto entityToDto(Review review){
        ReviewResponseDto dto = new ReviewResponseDto();
        String creatorName = review.getCreator() != null ?
                review.getCreator().getUsername() : "Deleted User";
        dto.setReviewCreatorUsername(creatorName);
        dto.setComment(review.getComment());
        dto.setDate(review.getCreated());
        dto.setId(review.getId());
        if(review.getCreator() != null && review.getCreator().getProfilePicture() != null) {
            dto.setCreatorProfilePictureId(review.getCreator().getProfilePicture().getId());
        }
        return dto;
    }

    private Review dtoToEntity(ReviewRequestDto dto){
        Review review = new Review();
        review.setComment(dto.getComment());
        review.setCreator(userRepository.getByUsername(dto.getReviewCreatorUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found.")));
        review.setLesson(lessonRepository.getById(dto.getLessonId()));
        if(review.getLesson() == null){
            throw new ResourceNotFoundException("Lesson not found.");
        }
        return review;
    }
}
