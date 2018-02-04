package org.lpro.boundary;

import org.lpro.entity.Commande;
import org.lpro.entity.Sandwich;
import org.lpro.entity.SandwichCommande;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import org.lpro.provider.Secured;

@Stateless
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("Commandes")
public class CommandeRepresentation {

    @Inject
    CommandeRessource commandeRessource;

    @Inject
    SandwichCommandeRessource sandwichCommandeRessource;

    @Inject
    SandwichManager sm;

    @Context
    UriInfo uriInfo;
    
    @POST
    @Secured
    public Response addCommande(@Valid Commande commande) {
        commande.setPrix("0");
        commande.setStatut("Attente de payement");
        Commande newCommande = this.commandeRessource.save(commande);
        URI uri = uriInfo.getAbsolutePathBuilder().path(newCommande.getId()).build();
        return Response.created(uri)
                .entity(newCommande)
                .build();
    }

    @GET
    @Path("/{commandeId}")
    public Response getOneCommande(@PathParam("commandeId") String commandeID,
            @DefaultValue("") @QueryParam("token") String tokenParam,
            @DefaultValue("") @HeaderParam("x-lbs-token") String tokenHeader) {
        Commande cmde = this.commandeRessource.findById(commandeID);
        if(cmde == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }    
        if(tokenParam.isEmpty() && tokenHeader.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String token = (tokenParam.isEmpty()) ? tokenHeader : tokenParam;
        Boolean isTokenValide = cmde.getToken().equals(token);
        
        if(!isTokenValide) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            return Response.ok(buildCommandeObject(cmde)).build();
        }
    }
    
        @GET
    @Path("{id}/Sandwichs")
    public Response getCommandeSandwichs(@PathParam("id") String id,
                                         @DefaultValue("") @QueryParam("token") String tokenParam,
                                         @DefaultValue("") @HeaderParam("x-lbs-token") String tokenHeader,
                                         @Context UriInfo uriInfo) {
        Commande c = this.commandeRessource.findById(id);
        Set<SandwichCommande> sc = c.getSandwichCommande();

        if(c == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if(tokenParam.isEmpty() && tokenHeader.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String token = (tokenParam.isEmpty()) ? tokenHeader : tokenParam;
        Boolean isTokenValide = c.getToken().equals(token);
        if(!isTokenValide) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            sc.forEach((res) -> {
                jab.add(SandwichRessource.buildJsonCommande(res.getSandwich(), res.getTaille(), res.getQte()));
            });
            JsonObject json = Json.createObjectBuilder()
                    .add("type", "collection")
                    .add("meta", Json.createObjectBuilder().add("count", sc.size()).build())
                    .add("sandwichs", jab.build())
                    .build();

            return Response.ok(json).build();
        }

    }

    @GET
    @Path("/{commandeId}/Facture")
    public Response getOneCommandeFacture(@PathParam("commandeId") String commandeID,
                                   @DefaultValue("") @QueryParam("token") String tokenParam,
                                   @DefaultValue("") @HeaderParam("x-lbs-token") String tokenHeader) {
        Commande cmde = this.commandeRessource.findById(commandeID);
        if(cmde == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if(tokenParam.isEmpty() && tokenHeader.isEmpty()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String token = (tokenParam.isEmpty()) ? tokenHeader : tokenParam;
        Boolean isTokenValide = cmde.getToken().equals(token);
        if(!isTokenValide) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            if(cmde.getStatut().equals("Commande Payee et envoyee")) {
                return Response.ok(buildFactureObject(cmde)).build();
            }else{
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
    }

    @POST
    @Secured
    @Path("{id}/Sandwich")
    public Response addSandwich(@PathParam("id") String id,
                                
        JsonObject request,
        @DefaultValue("") @QueryParam("token") String tokenParam,
        @DefaultValue("") @HeaderParam("x-lbs-token") String tokenHeader) {
        Commande cmde = this.commandeRessource.findById(id);
        String idSandwich = null;
        String qte = null;
        String taille = null;
        if(!request.isEmpty()){
            idSandwich = request.getString("idsandwich");
            qte = request.getString("qte");
            taille = request.getString("taille");
        }
        String token = (tokenParam.isEmpty()) ? tokenHeader : tokenParam;
        Boolean isTokenValide = cmde.getToken().equals(token);
        if(!isTokenValide) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            if(!cmde.getStatut().equals("Commande Payee et envoyee")) {
                if (idSandwich != null) {
                    Sandwich s = this.sm.findById(idSandwich);
                    SandwichCommande sc = new SandwichCommande(cmde, s, qte, taille);
                    sandwichCommandeRessource.save(sc);
                    s.addCommande(sc);
                    cmde.addSandwich(sc);
                    return Response.status(Response.Status.CREATED).build();
                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }else{
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
    }
    
    @POST
    @Secured
    @Path("{id}/Paye")
    public Response payeCommande(@PathParam("id") String id,
                                 JsonObject infoCarte,
                                 @DefaultValue("") @QueryParam("token") String tokenParam,
                                 @DefaultValue("") @HeaderParam("x-lbs-token") String tokenHeader) {
        Commande cmde = this.commandeRessource.findById(id);
        String numCarte = infoCarte.getString("numCarte");
        String dateExp = infoCarte.getString("tailleExp");
        String token = (tokenParam.isEmpty()) ? tokenHeader : tokenParam;
        Boolean isTokenValide = cmde.getToken().equals(token);
        if(!isTokenValide) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            if(!cmde.getStatut().equals("Commande Payee et envoyee")) {
                //On verifie Coordonnée bancaire
                if (!numCarte.isEmpty() && !dateExp.isEmpty()) {
                    cmde.setStatut("Commande Payee et envoyee");
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = new Date();
                    cmde.setDatePaiement(dateFormat.format(date));

                    return Response.status(Response.Status.ACCEPTED).build();
                } else {
                    return Response.status(Response.Status.NO_CONTENT).build();
                }
            }else{
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
    }

    @DELETE
    @Secured
    @Path("{id}/Sandwich")
    public Response deleteSandwich(@PathParam("id") String id,
        @QueryParam("idsandwich") String idSandwich,
        @DefaultValue("") @QueryParam("token") String tokenParam,
        @DefaultValue("") @HeaderParam("x-lbs-token") String tokenHeader) {
        Commande cmde = this.commandeRessource.findById(id);
        String token = (tokenParam.isEmpty()) ? tokenHeader : tokenParam;
        Boolean isTokenValide = cmde.getToken().equals(token);
        if(!isTokenValide) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            if(!cmde.getStatut().equals("Commande Payee et envoyee")) {
                if (idSandwich != null) {
                    SandwichCommande sc = this.sandwichCommandeRessource.findById(cmde.getId(), idSandwich);
                    this.sandwichCommandeRessource.delete(sc);
                    return Response.status(Response.Status.OK).build();

                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }else{
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
    }

    @PUT
    @Secured
    @Path("{id}")
    public Response modifCommande(@PathParam("id") String id,
        JsonObject newDate,
        @DefaultValue("") @QueryParam("token") String tokenParam,
        @DefaultValue("") @HeaderParam("x-lbs-token") String tokenHeader) {
        Commande cmde = this.commandeRessource.findById(id);
        String dateLivr = newDate.getString("dateLivraison");
        String heureLivr = newDate.getString("heureLivraison");
        String token = (tokenParam.isEmpty()) ? tokenHeader : tokenParam;
        Boolean isTokenValide = cmde.getToken().equals(token);
        if(!isTokenValide) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            if(!cmde.getStatut().equals("Commande Payee et envoyee")) {
                if (dateLivr != null) {
                    cmde.setDateLivraison(dateLivr);
                    if(heureLivr != null){
                        cmde.setHeureLivraison(heureLivr);
                    }
                    commandeRessource.update(cmde);
                    return Response.status(Response.Status.OK).build();

                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }else{
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
    }

    @PUT
    @Secured
    @Path("{id}/Modif")
    public Response modifCommande(@PathParam("id") String id,
        @DefaultValue("") @QueryParam("idsandwich") String idSandwich,
        @DefaultValue("") @QueryParam("taille") String taille,
        @DefaultValue("") @QueryParam("qte") String qte,
        @DefaultValue("") @QueryParam("token") String tokenParam,
        @DefaultValue("") @HeaderParam("x-lbs-token") String tokenHeader) {
        Commande cmde = this.commandeRessource.findById(id);
        String token = (tokenParam.isEmpty()) ? tokenHeader : tokenParam;
        Boolean isTokenValide = cmde.getToken().equals(token);
        if (!isTokenValide) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            if (!cmde.getStatut().equals("Commande Payee et envoyee")) {
                if (!idSandwich.isEmpty()) {
                    SandwichCommande sc = this.sandwichCommandeRessource.findById(cmde.getId(), idSandwich);
                    if(!qte.isEmpty())
                         sc.setQte(qte);
                    if(!taille.isEmpty())
                        sc.setTaille(taille);
                    this.sandwichCommandeRessource.update(sc);
                    return Response.status(Response.Status.OK).build();

                } else {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            } else {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
    }

    private JsonObject buildCommandeObject(Commande c) {
        return Json.createObjectBuilder()
                .add("commande", buildJsonForCommande(c))
                .build();
    }

    private JsonObject buildFactureObject(Commande c) {
        return Json.createObjectBuilder()
                .add("commande", buildJsonForFacture(c))
                .build();
    }

    private JsonObject buildJsonForCommande(Commande c) {

        JsonArrayBuilder sandwichs = Json.createArrayBuilder();
        c.getSandwichCommande().forEach((s) -> {
            JsonObject sandwich = Json.createObjectBuilder()
                    .add("id", s.getSandwich().getId())
                    .add("nom", s.getSandwich().getNom())
                    .add("taille", s.getTaille())
                    .add("qte", s.getQte())
                    .build();
            sandwichs.add(sandwich);
        });

        return Json.createObjectBuilder()
                .add("id", c.getId())
                .add("nom_client", c.getNom())
                .add("mail_client", c.getMail())
                .add("livraison", buildJsonForLivraison(c))
                .add("token", c.getToken())
                .add("sandwich", sandwichs)
                .add("prix", c.getPrix())
                .add("statut", c.getStatut())
                .build();
    }
    
    private JsonObject buildJsonForFacture(Commande c) {

        JsonArrayBuilder sandwichs = Json.createArrayBuilder();
        c.getSandwichCommande().forEach((s) -> {
            JsonObject sandwich = Json.createObjectBuilder()
                    .add("id", s.getSandwich().getId())
                    .add("nom", s.getSandwich().getNom())
                    .add("taille", s.getTaille())
                    .add("qte", s.getQte())
                    .build();
            sandwichs.add(sandwich);
        });

        return Json.createObjectBuilder()
                .add("id", c.getId())
                .add("nom_client", c.getNom())
                .add("mail_client", c.getMail())
                .add("livraison", buildJsonForLivraison(c))
                .add("token", c.getToken())
                .add("sandwich", sandwichs)
                .add("prix", c.getPrix())
                .add("statut", c.getStatut())
                .add("date paiement", c.getDatePaiement())
                .build();
    }
            private JsonObject buildJsonForPaiement(Commande c) {
        return Json.createObjectBuilder()
                .add("date paiement", c.getDatePaiement())
                .add("heure paiement", c.getDatePaiement())
                .build();
    }

    private JsonObject buildJsonForLivraison(Commande c) {
        return Json.createObjectBuilder()
                .add("date", c.getDateLivraison())
                .add("heure", c.getHeureLivraison())
                .build();
    }
}
