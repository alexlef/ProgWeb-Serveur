/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lpro.boundary;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.lpro.entity.Sandwich;

/**
 *
 * @author alex
 */
@Stateless
@Path("sandwichs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SandwichRessource {
    @Inject
    SandwichManager sm;
    
    @GET
    public Response getSandwichs(@QueryParam("t")String ptype,@QueryParam("img") int img,@QueryParam("page") int page,@DefaultValue("20") @QueryParam("size") int size){
        GenericEntity<List<Sandwich>> liste = null;
        if(img==1 || ptype != null){
            if(img == 1 && ptype != null){
                liste = new GenericEntity<List<Sandwich>>(this.sm.findAllImg(ptype)){
                    
                };
            }else{
                if(img == 1){
                    liste = new GenericEntity<List<Sandwich>>(this.sm.findByImg()){
                    
                    };
                }
                if(ptype != null){
                    liste = new GenericEntity<List<Sandwich>>(this.sm.findTypePain(ptype)){
                    
                    };
                }
            }
        }
        else if(page<=0){
            liste = new GenericEntity<List<Sandwich>>(this.sm.findByPage(1, size)){};
        }
        else if(page >0){
            liste = new GenericEntity<List<Sandwich>>(this.sm.findByPage(page, size)){};
        }
        else{
            liste = new GenericEntity<List<Sandwich>>(this.sm.findAll()){
                    
             };
        }
        return Response.ok(liste).build();
    }
    
    @GET
    @Path("{id}")
    public Response getOneSandwich(@PathParam("id") long id, @Context UriInfo uriInfo) {
        return Optional.ofNullable(sm.findById(id))
                .map(c -> Response.ok(c).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
      
    @POST
    public Response newSandwich(@Valid Sandwich c, @Context UriInfo uriInfo) {
        Sandwich newOne = this.sm.save(c);
        long id = newOne.getId();
        URI uri = uriInfo.getAbsolutePathBuilder().path("/"+id).build();
        return Response.created(uri).build();
    }
            
    @DELETE
    @Path("{id}")
    public Response suppression(@PathParam("id") long id) {
        this.sm.delete(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
    
    @PUT
    @Path("{id}")
    public Sandwich update(@PathParam("id") long id, Sandwich c) {
        c.setId(id);
        return this.sm.save(c);
    }

    private JsonObject categorie2Json(Sandwich c) {
        JsonObject json = Json.createObjectBuilder()
                .add("type", "resource")
                .add("sandwich", buildJson(c))
                .build();
        return json;
    }
    
    private JsonArray getSandwichList() {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        this.sm.findAll().forEach((c) -> {
            jab.add(buildJson(c));
            });
        return jab.build();
    }
    
    private JsonObject buildJson(Sandwich c) {
        return Json.createObjectBuilder()
                .add("id",c.getId())
                .add("nom", c.getNom())
                .add("desc", c.getDescription())
                .add("type_pain", c.getType_pain())
                .add("img",c.getImg())
                .build();
    }
}
