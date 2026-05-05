package com.example.customer.exceptions;

public class CustomerNotFoundException extends RuntimeException{
     public CustomerNotFoundException(Long id){
        super(String.format("Customer not found with id: %s", id));
     }
}
