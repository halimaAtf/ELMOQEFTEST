package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter


public class paiments {
@Id

private Long id;

    @Column(unique = true, nullable = false)
    private String date;


}
//getter & setter

