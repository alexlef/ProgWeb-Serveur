/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lpro.entity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author alex
 */
@Entity
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@NamedQuery(name="Sandwich.findAll",query="SELECT c FROM Sandwich c")
public class Sandwich implements Serializable {

    @Id
    private Long id;
    @NotNull
    private String nom;
    @NotNull
    private String description;
    @NotNull
    private String type_pain;
    @NotNull
    private String img;
    
    public Sandwich(){
        
    }

    public Sandwich(Long id, String nom, String description, String type_pain, String img) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.type_pain = type_pain;
        this.img = img;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType_pain() {
        return type_pain;
    }

    public void setType_pain(String type_pain) {
        this.type_pain = type_pain;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }
    
    
    
    
}
