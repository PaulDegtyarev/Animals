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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.out;

public class UpdateUserById extends AbstractVerticle {

    private static  final PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(5432)
            .setHost("5.35.94.60")
            .setDatabase("animal-chipization")
            .setUser("postgres")
            .setPassword("105xPyj");

    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);

        router.put("/accounts/:accountId").handler(this::updateUserById);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        out.println("HTTP server UpdateUserById started on port 8080");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private void updateUserById(RoutingContext routingContext) {
        routingContext.request().body().onComplete(bufferAsyncResult -> {

            if (bufferAsyncResult.succeeded()) {

                JsonObject info = new JsonObject(bufferAsyncResult.result());

                String newFirstName = info.getString("firstName");
                String newLastName = info.getString("lastName");
                String newEmail = info.getString("email");
                String newPassword = info.getString("password");
                String newRole = info.getString("role");

                String accountIdParam = routingContext.pathParam("accountId");
                Integer accountId = null;
                if (!"null".equals(accountIdParam)) {
                    accountId = Integer.parseInt(accountIdParam);
                }

                String token = routingContext.request().getHeader("token");

                String emailPattern = "^[a-z0-9]+@[a-z0-9]+(\\.[a-z]{2,})$";
                String roles[] = {"ADMIN", "CHIPPER", "USER"};
                ArrayList<String> rolesList = new ArrayList<>(Arrays.asList(roles));

                Session session = HibernateSessionFactoryUtil
                        .getSessionFactory()
                        .openSession();

                if (accountId == (null) || accountId <= 0) {
                    routingContext.response().setStatusCode(400).end("accountId cannot be null or less than 0");
                } else {
                    if (newFirstName == (null) || newFirstName.trim().isEmpty() || newLastName == (null) || newLastName.trim().isEmpty() || newEmail == (null) || newEmail.trim().isEmpty() || !newEmail.trim().matches(emailPattern) || !rolesList.contains(newRole) || newPassword.equals("null") || newPassword.trim().isEmpty()) {
                        routingContext.response().setStatusCode(400).end();
                    } else {
                        Query queryForUpdate = session.createQuery("FROM Users where id = :userId");
                        queryForUpdate.setParameter("userId", accountId);

                        List<Users> usersForUpdate = queryForUpdate.list();

                        Query query = session.createQuery("from Users where usertoken = :userToken");
                        query.setParameter("userToken", token);
                        List<Users> usersList = query.list();

                        if (usersList.isEmpty()){
                            routingContext.response().setStatusCode(401).end("You are not authorized");
                        } else {
                            Users user = usersList.get(0);
                            String storedUserRole = user.getRole();

                            if (storedUserRole.equals("CHIPPER") || storedUserRole.equals("USER")){
                                Integer storedId = user.getId();

                                out.println(storedUserRole);
                                out.println(storedId);
                                out.println(accountId);

                                if (!storedId.equals(accountId)){
                                    routingContext.response().setStatusCode(403).end("Updating an account that is not yours");
                                } else {
                                    session.beginTransaction();

                                    user.setFirstName(newFirstName);
                                    user.setLastName(newLastName);
                                    user.setEmail(newEmail);
                                    user.setPassword(newPassword);
                                    user.setRole(newRole);

                                    session.update(user);
                                    session.getTransaction().commit();

                                    routingContext.response().setStatusCode(200).end("Successfully updated!");

                                }
                            } else if (storedUserRole.equals("ADMIN")){
                                if (usersForUpdate.isEmpty()){
                                    routingContext.response().setStatusCode(404).end("Account not found");
                                } else {
                                    session.beginTransaction();
                                    try {
                                        Query queryToCheckEmail = session.createQuery("from Users where email = :userEmail");
                                        queryToCheckEmail.setParameter("userEmail", newEmail);

                                        List<Users> usersListToCheckEmail = queryToCheckEmail.list();
                                        if (!usersListToCheckEmail.isEmpty()){
                                            routingContext.response().setStatusCode(409).end("An account with the same email already exists");
                                        } else {
                                            Users userForUpdate = usersForUpdate.get(0);
                                            userForUpdate.setFirstName(newFirstName);
                                            userForUpdate.setLastName(newLastName);
                                            userForUpdate.setEmail(newEmail);
                                            userForUpdate.setPassword(newPassword);
                                            userForUpdate.setRole(newRole);

                                            session.update(userForUpdate);

                                            session.getTransaction().commit();

                                            routingContext.response().end("Successfully updated!");
                                        }
                                    } catch (Exception e){
                                        session.getTransaction().rollback();
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
                session.close();
            }
        });
    }

}
