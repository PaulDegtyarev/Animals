package server;

import hibernate.Animal;
import hibernate.Users;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;

import static java.lang.System.out;

public class ForAnimals extends AbstractVerticle {
    private static  final PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(5432)
            .setHost("5.35.94.60")
            .setDatabase("animal-chipization")
            .setUser("postgres")
            .setPassword("105xPyj");

    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);

        router.get("/animals/:animalId").handler(this::getAnimalById);


        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        out.println("HTTP server started on port 8080");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private void getAnimalById(RoutingContext routingContext) {
        routingContext.request().body().onComplete(bufferAsyncResult -> {

            if (bufferAsyncResult.succeeded()){
                String userToken = routingContext.request().getHeader("token");

                Integer animalId = null;
                String animalIdParam = routingContext.pathParam("animalId");
                if (!"null".equals(animalIdParam)){
                    animalId = Integer.parseInt(animalIdParam);
                }
                out.println(animalId);

                Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
                configuration.addAnnotatedClass(Animal.class);
                configuration.addAnnotatedClass(Users.class);
                Session session = configuration
                        .buildSessionFactory()
                        .openSession();
                try {
                    session.beginTransaction();
                    if (animalId == null || animalId <= 0) {
                        routingContext.response().setStatusCode(400).end("animal id cannot be less than or equal to 0 or null");
                    } else {
                        Query queryToCheckUser = session.createQuery("FROM Users where usertoken = :userToken");
                        queryToCheckUser.setParameter("userToken", userToken);

                        List<Users> listToCheckUser = queryToCheckUser.list();
                        if (listToCheckUser.isEmpty()){
                            routingContext.response().setStatusCode(401).end("You are not authorized");
                        } else {
                            Query queryToGetAnimal = session.createQuery("FROM Animal where id = :animalId");
                            queryToGetAnimal.setParameter("animalId", animalId);

                            List<Animal> listToGetAnimal = queryToGetAnimal.list();
                            if (listToGetAnimal.isEmpty()){
                                routingContext.response().setStatusCode(404).end("animal not found");
                            } else {
                                Animal animalToGetById = listToGetAnimal.get(0);

                                Integer storedAnimalId = animalToGetById.getId();
                                BigInteger[] storedAnimalTypes = animalToGetById.getAnimaltypes();
                                Float storedAnimalWeight = animalToGetById.getWeight();
                                Float storedAnimalLength = animalToGetById.getLength();
                                Float storedAnimalHeight = animalToGetById.getHeight();
                                String storedAnimalGender = animalToGetById.getGender();
                                String storedAnimalLifeStatus = animalToGetById.getLifeStatus();
                                Timestamp storedAnimalChippingDateTime = animalToGetById.getChippingDateTime();
                                Integer storedChipperId = animalToGetById.getChipperid().getId();
                                Integer storedAnimalChippingLocationId = animalToGetById.getChippingLocationId();
                                BigInteger[] storedAnimalVisitedLocations = animalToGetById.getVisitedlocations();
                                Timestamp storedAnimalDeathDateTime = animalToGetById.getDeathDateTime();

                                JsonObject animalJson = new JsonObject();
                                animalJson.put("id", storedAnimalId);
                                animalJson.put("animalTypes", storedAnimalTypes);
                                animalJson.put("weight", storedAnimalWeight);
                                animalJson.put("length", storedAnimalLength);
                                animalJson.put("height", storedAnimalHeight);
                                animalJson.put("gender", storedAnimalGender);
                                animalJson.put("lifeStatus", storedAnimalLifeStatus);
                                animalJson.put("chippingDateTime", storedAnimalChippingDateTime);
                                animalJson.put("chipperId", storedChipperId);
                                animalJson.put("chippingLocationId", storedAnimalChippingLocationId);
                                animalJson.put("visitedLocations", storedAnimalVisitedLocations);
                                animalJson.put("deathDateTime", storedAnimalDeathDateTime);

                                session.getTransaction().commit();
                                routingContext.response().setStatusCode(200).end(animalJson.encode());

                            }
                        }
                    }

                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    session.close();
                }
            }
        });
    }
}
