package com.example.customer.events;

import com.example.customer.enums.CustomerEventType;

public record CustomerEvent(CustomerEventType customerEventType, Long customerId, String name, String email) {}
