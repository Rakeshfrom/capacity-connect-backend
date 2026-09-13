package com.capacityconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.util.LinkedMultiValueMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakRoleService {
    @Value("${keycloak.url:https://keycloak-production-66a8.up.railway.app}") private String keycloakUrl;
    @Value("${keycloak.realm:capacity-connect}") private String realm;
    @Value("${keycloak.admin-client-id:capacity-connect-admin}") private String adminClientId;
    @Value("${keycloak.admin-client-secret:}") private String adminClientSecret;
    private final RestClient restClient=RestClient.create();

    public void assignTrainerRoleByEmail(String email){
        String token=getAdminToken(); String clean=email.trim(); List<?> users=restClient.get().uri(keycloakUrl+"/admin/realms/"+realm+"/users?email="+org.springframework.web.util.UriUtils.encodeQueryParam(clean,java.nio.charset.StandardCharsets.UTF_8)).header("Authorization","Bearer "+token).retrieve().body(List.class);
        String id=null;
        if(users!=null) for(Object item:users) if(item instanceof Map<?,?> u && u.get("id")!=null && clean.equalsIgnoreCase(String.valueOf(u.get("email")))) {id=String.valueOf(u.get("id"));break;}
        if(id==null) throw new IllegalStateException("Keycloak user not found for trainer approval");
        Map<?,?> role=restClient.get().uri(keycloakUrl+"/admin/realms/"+realm+"/roles/TRAINER").header("Authorization","Bearer "+token).retrieve().body(Map.class);
        restClient.post().uri(keycloakUrl+"/admin/realms/"+realm+"/users/"+id+"/role-mappings/realm").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).body(List.of(role)).retrieve().toBodilessEntity();
    }

    private String getAdminToken(){var form=new LinkedMultiValueMap<String,String>();form.add("grant_type","client_credentials");form.add("client_id",adminClientId);form.add("client_secret",adminClientSecret);Map<?,?> r=restClient.post().uri(keycloakUrl+"/realms/"+realm+"/protocol/openid-connect/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);return String.valueOf(r.get("access_token"));}
}
