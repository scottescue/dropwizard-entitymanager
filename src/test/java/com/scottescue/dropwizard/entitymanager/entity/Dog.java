package com.scottescue.dropwizard.entitymanager.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "dogs")
public class Dog {
    @Id
    private String name;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="owner")
    private Person owner;

    @JsonProperty
    public String getName() {
        return name;
    }

    @JsonProperty
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty
    public Person getOwner() {
        return owner;
    }

    @JsonProperty
    public void setOwner(Person owner) {
        this.owner = owner;
    }
}
