package com.college.eventmanagement.service;

import com.college.eventmanagement.exception.BadRequestException;
import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.model.Event;
import com.college.eventmanagement.repository.EventRepository;
import com.college.eventmanagement.repository.RegistrationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public EventService(EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    public Event createEvent(Event event) {
        event.setId(null);
        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + id));
    }

    public List<Event> getByCategory(String category) {
        return eventRepository.findByCategoryIgnoreCase(category);
    }

    public List<Event> getByDateRange(LocalDate start, LocalDate end) {
        return eventRepository.findByEventDateBetween(start, end);
    }

    public Event updateEvent(Long id, Event updated) {
        Event existing = getEventById(id);
        long registered = registrationRepository.countByEventId(id);
        if (updated.getTotalSeats() < registered) {
            throw new BadRequestException("Total seats cannot be less than the "
                    + registered + " students already registered");
        }
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setEventDate(updated.getEventDate());
        existing.setVenue(updated.getVenue());
        existing.setCategory(updated.getCategory());
        existing.setTotalSeats(updated.getTotalSeats());
        return eventRepository.save(existing);
    }

    public void deleteEvent(Long id) {
        Event event = getEventById(id);
        if (registrationRepository.countByEventId(id) > 0) {
            throw new BadRequestException("Cannot delete an event that has registrations");
        }
        eventRepository.delete(event);
    }
}