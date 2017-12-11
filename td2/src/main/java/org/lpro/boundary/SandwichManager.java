/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lpro.boundary;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.lpro.entity.Sandwich;

/**
 *
 * @author alex
 */
@Stateless
public class SandwichManager {
    @PersistenceContext
    EntityManager em;

    public Sandwich findById(long id) {
        return this.em.find(Sandwich.class, id);
    }

    public List<Sandwich> findAll() {
        Query q = this.em.createNamedQuery("Sandwich.findAll", Sandwich.class);
        q.setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);
        return q.getResultList();
    }
    
    public List<Sandwich> findTypePain(String type){
        Query q = em.createQuery("SELECT s FROM Sandwich s WHERE s.type_pain = :type");
        q.setParameter("type", type);
        return q.getResultList();
    }
    
    public List<Sandwich> findAllImg(String type){
        Query q=em.createQuery("SELECT s FROM Sandwich s WHERE s.type_pain = :type AND s.img is not NULL");
        q.setParameter("type",type);
        return q.getResultList();
    }

    public List<Sandwich> findByImg(){
        Query q = em.createQuery("SELECT s FROM Sandwich s WHERE s.img is not NULL");
        return q.getResultList();
    }
    
    public List<Sandwich> findByPage(int p,int s){
        Query nbelem = em.createQuery("Select count(s.id) from Sandwich s");
        long nbelement = (long) nbelem.getSingleResult();
        int nbp =(int) (nbelement/s)+1;
        if(nbp<p){
            p = nbp;
        }
        Query q =this.em.createNamedQuery("Sandwich.findAll",Sandwich.class);
        q.setFirstResult((p-1)*s);
        q.setMaxResults(s);
        return q.getResultList();
    }
    
    public Sandwich save(Sandwich c) {
        return this.em.merge(c);
    }

    public void delete(long id) {
        try {
            Sandwich ref = this.em.getReference(Sandwich.class, id);
            this.em.remove(ref);
        } catch (EntityNotFoundException enfe) {
            // rien à faire   
        }
    }
}
