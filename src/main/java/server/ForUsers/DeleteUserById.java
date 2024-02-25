package server.ForUsers;

import hibernate.Animal;
import hibernate.Users;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;

import static java.lang.System.out;

public class DeleteUserById extends AbstractVerticle {

    private static  final PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(5432)
            .setHost("5.35.94.60")
            .setDatabase("animal-chipization")
            .setUser("postgres")
            .setPassword("105xPyj");

    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);

        router.delete("/accounts/:accountId").handler(this::deleteUserById);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        out.println("HTTP server DeleteUserById started on port 8080");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private void deleteUserById(RoutingContext routingContext) {
        routingContext.request().body().onComplete(bufferAsyncResult -> {
            if (bufferAsyncResult.succeeded()) {
                String accountIdParam = routingContext.pathParam("accountId");
                Integer accountId = null;
                if (!"null".equals(accountIdParam)) {
                    accountId = Integer.parseInt(accountIdParam);
                }

                String userToken = routingContext.request().getHeader("token");

                if (accountId == null || accountId <= 0){
                    routingContext.response().setStatusCode(400).end("id cannot be null or less than or equal to 0");
                } else {
                    Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
                    configuration.addAnnotatedClass(Animal.class);
                    configuration.addAnnotatedClass(Users.class);

                    Session session = configuration
                            .buildSessionFactory()
                            .openSession();

                    session.beginTransaction();

                    try {
                        Query querycheckChippersWithAnimals = session.createQuery("SELECT COUNT(*) FROM Users u JOIN Animal a ON u.id = a.chipperid.id WHERE u.id = :userId");
                        querycheckChippersWithAnimals.setParameter("userId", accountId);

                        Integer count = Math.toIntExact((Long) querycheckChippersWithAnimals.uniqueResult());

                        if (count > 0 ){
                            routingContext.response().setStatusCode(400).end("Account linked to animal");
                        } else if (count.equals(0)){
                            Query queryToCheckUser = session.createQuery("FROM Users where usertoken = :userToken");
                            queryToCheckUser.setParameter("userToken", userToken);

                            List<Users> listToCheckUser = queryToCheckUser.list();
                            if (listToCheckUser.isEmpty()){
                                routingContext.response().setStatusCode(401).end("You are not authorized");
                            } else {
                                Users user = listToCheckUser.get(0);

                                String storedUserRole = user.getRole();
                                Integer storedUserId = user.getId();

                                Query queryToCheckUserAvailability = session.createQuery("FROM Users where id = :userId");
                                queryToCheckUserAvailability.setParameter("userId", accountId);

                                List<Users> listToCheckUserAvailability = queryToCheckUserAvailability.list();


                                if ((storedUserRole.equals("CHIPPER") || storedUserRole.equals("USER"))){
                                    if (!storedUserId.equals(accountId) || listToCheckUserAvailability.isEmpty()){
                                        routingContext.response().setStatusCode(403).end();
                                    } else {
                                        Users userForDelete = listToCheckUserAvailability.get(0);
                                        session.delete(userForDelete);
                                        session.getTransaction().commit();
                                        routingContext.response().setStatusCode(200).end("OK");
                                    }
                                } else if (storedUserRole.equals("ADMIN")){
                                    if (listToCheckUserAvailability.isEmpty()){
                                        routingContext.response().setStatusCode(404).end("account not found");
                                    } else {
                                        Users userForDelete = listToCheckUserAvailability.get(0);
                                        session.delete(userForDelete);
                                        session.getTransaction().commit();
                                        routingContext.response().setStatusCode(200).end("OK");
                                    }

                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        session.close();
                    }
                }
            }
        });
    }

}
