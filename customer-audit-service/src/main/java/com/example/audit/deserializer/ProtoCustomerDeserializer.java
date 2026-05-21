package com.example.audit.deserializer;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import com.example.customer.protobuf.event.CustomerEvent;
@Slf4j
public class ProtoCustomerDeserializer implements Deserializer<CustomerEvent> {
    @Override
    public CustomerEvent deserialize(final String topic, byte[] data) {
        if(data==null || data.length==0){
            log.info("No data received");
            return null;
        }
        try
        {
            return CustomerEvent.parseFrom(data);
        }catch (InvalidProtocolBufferException  e) {
            throw new SerializationException("Failed to deserialize protobuf CustomerEvent",e);
        }
    }
}