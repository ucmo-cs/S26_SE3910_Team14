package com.bankscheduling.appointment.controller;

import com.bankscheduling.appointment.dto.publicbooking.BookingRequestDto;
import com.bankscheduling.appointment.dto.publicbooking.BookingResponseDto;
import com.bankscheduling.appointment.dto.publicbooking.PublicBranchDto;
import com.bankscheduling.appointment.dto.publicbooking.PublicServiceTypeDto;
import com.bankscheduling.appointment.dto.publicbooking.PublicTimeslotsDto;
import com.bankscheduling.appointment.service.PublicBookingService;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/booking")
public class PublicBookingController {

    private final PublicBookingService publicBookingService;

    public PublicBookingController(PublicBookingService publicBookingService) {
        this.publicBookingService = publicBookingService;
    }

    @GetMapping("/topics")
    public List<PublicServiceTypeDto> getTopics() {
        return publicBookingService.getTopics();
    }

    @GetMapping("/branches")
    public List<PublicBranchDto> getBranches(@RequestParam(required = false) Long topicId) {
        return publicBookingService.getBranches(topicId);
    }

    @GetMapping("/times")
    public PublicTimeslotsDto getAvailableTimes(
            @RequestParam Long branchId,
            @RequestParam Long topicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return publicBookingService.getAvailableTimes(branchId, topicId, date);
    }

    @PostMapping("/book")
    public BookingResponseDto book(@Valid @RequestBody BookingRequestDto request) {
        return publicBookingService.book(request);
    }
}
