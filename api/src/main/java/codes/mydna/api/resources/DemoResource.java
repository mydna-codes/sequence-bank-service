package codes.mydna.api.resources;

import codes.mydna.api.resources.definitions.DemoResourceDefinition;
import com.mjamsek.auth.keycloak.annotations.RealmRolesAllowed;
import com.mjamsek.auth.keycloak.annotations.SecureResource;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Path("demo")
@Tag(name = "Other")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@SecureResource
@RequestScoped
public class DemoResource implements DemoResourceDefinition {

    @Override
    @RealmRolesAllowed({"mdc_admin", "mdc_user", "mdc_pro_user"})
    public Response demo() {
        StringBuilder content = new StringBuilder();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("projectInfo.json")) {
            if(inputStream != null){
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(content.toString()).build();
    }

}
