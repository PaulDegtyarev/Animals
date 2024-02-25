package server.ForUsers;

import hibernate.Users;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import org.hibernate.Session;
import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

import static java.lang.System.out;

public class GetUserById extends AbstractVerticle {

    private static  final PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(5432)
            .setHost("5.35.94.60")
            .setDatabase("animal-chipization")
            .setUser("postgres")
            .setPassword("105xPyj");

    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);

        router.get("/accounts/:accountId").handler(this::getUserById);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        out.println("HTTP server GetUserById started on port 8080");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private void getUserById(RoutingContext routingContext) {

        String accountIdParam = routingContext.pathParam("accountId");
        Integer userId = null;
        if (!"null".equals(accountIdParam)) {
            userId = Integer.parseInt(accountIdParam);
        }

        String userToken = routingContext.request().getHeader("token");

        Session session = HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession();

        if (userId == null || userId <= 0) {
            routingContext.response().setStatusCode(400).end("User id cannot be absent or less than or equal to 0!");
        } else {
            try {
                Query queryForAuthorization = session.createQuery("from Users where usertoken = :userToken");
                queryForAuthorization.setParameter("userToken", userToken);

                List<Users> usersForAuthorization = queryForAuthorization.list();

                if (usersForAuthorization.isEmpty()){
                    routingContext.response().setStatusCode(401).end("You are not authorize");
                } else{
                    Users user = usersForAuthorization.get(0);

                    String storedUserRole = user.getRole();
                    Integer storedId = user.getId();

                    if (!storedId.equals(userId) && (storedUserRole.equals("CHIPPER") || storedUserRole.equals("USER"))){

                        routingContext.response().setStatusCode(403).end("Insufficient rights or someone else's account or account does not exist");

                    } else if ((storedId.equals(userId)) && (storedUserRole.equals("CHIPPER") || storedUserRole.equals("USER"))){
                        Query queryForNotAdmin = session.createQuery("from Users where id = :idUser");
                        queryForNotAdmin.setParameter("idUser", userId);

                        List<Users> resultForNotAdmin = queryForNotAdmin.list();


                        if (resultForNotAdmin.isEmpty()){
                            routingContext.response().setStatusCode(403).end();
                        } else {
                            Users userNotAdmin = resultForNotAdmin.get(0);

                            Integer idNotAdmin = userNotAdmin.getId();
                            String firstnameNotAdmin = userNotAdmin.getFirstName();
                            String lastnameNotAdmin = userNotAdmin.getLastName();
                            String emailNotAdmin = userNotAdmin.getEmail();
                            String roleNotAdmin = userNotAdmin.getRole();

                            JsonObject userJson = new JsonObject();
                            userJson.put("id", idNotAdmin);
                            userJson.put("firstName", firstnameNotAdmin);
                            userJson.put("lastName", lastnameNotAdmin);
                            userJson.put("email", emailNotAdmin);
                            userJson.put("role", roleNotAdmin);

                            routingContext.response().setStatusCode(200).end(userJson.encode());
                        }
                    } else if (storedUserRole.equals("ADMIN")){
                        Query queryForAdmin = session.createQuery("from Users where id = :idUser");
                        queryForAdmin.setParameter("idUser", userId);

                        List<Users> usersForAdmin = queryForAdmin.list();

                        if (usersForAdmin.isEmpty()){
                            routingContext.response().setStatusCode(404).end("Account does not exist");
                        } else {

                            Users userForAdmin = usersForAdmin.get(0);

                            Integer idForAdmin = userForAdmin.getId();
                            String firstnameForAdmin = userForAdmin.getFirstName();
                            String lastnameForAdmin = userForAdmin.getLastName();
                            String emailForAdmin = userForAdmin.getEmail();
                            String roleForAdmin = userForAdmin.getRole();

                            JsonObject userJson = new JsonObject();
                            userJson.put("id", idForAdmin);
                            userJson.put("firstName", firstnameForAdmin);
                            userJson.put("lastName", lastnameForAdmin);
                            userJson.put("email", emailForAdmin);
                            userJson.put("role", roleForAdmin);

                            routingContext.response().setStatusCode(200).end(userJson.encode());
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
