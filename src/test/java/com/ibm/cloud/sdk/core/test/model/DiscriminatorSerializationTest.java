/**
 * (C) Copyright IBM Corp. 2015, 2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.ibm.cloud.sdk.core.test.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.gson.JsonSyntaxException;
import com.ibm.cloud.sdk.core.test.model.generated.Animal;
import com.ibm.cloud.sdk.core.test.model.generated.AnimalCat;
import com.ibm.cloud.sdk.core.test.model.generated.AnimalDog;
import com.ibm.cloud.sdk.core.test.model.generated.AnimalIguana;
import com.ibm.cloud.sdk.core.test.model.generated.Car;
import com.ibm.cloud.sdk.core.test.model.generated.QueryAggregation;
import com.ibm.cloud.sdk.core.test.model.generated.QueryNestedAggregation;
import com.ibm.cloud.sdk.core.test.model.generated.QueryResponse;
import com.ibm.cloud.sdk.core.test.model.generated.QueryTermAggregation;
import com.ibm.cloud.sdk.core.test.model.generated.Truck;
import com.ibm.cloud.sdk.core.test.model.generated.Vehicle;
import com.ibm.cloud.sdk.core.util.GsonSingleton;

/**
 * This class contains tests of our DynamicModelTypeAdapterFactory.
 * The test data consists of a few dynamic models that were generated by the java generator
 * and then copied here to this project.
 */
public class DiscriminatorSerializationTest {
  private boolean displayOutput = false;

  private void log(String msg) {
    if (displayOutput) {
      System.out.println(msg);
    }
  }

  private String serialize(Object obj) {
    return GsonSingleton.getGson().toJson(obj);
  }

  private <T> T deserialize(String json, Class<T> clazz) {
    return GsonSingleton.getGson().fromJson(json, clazz);
  }

  private <T> void testSerDeser(Object model, Class<T> baseClass, Class<? extends T> subClass) {
    String jsonString = serialize(model);
    log("serialized " + model.getClass().getSimpleName() + ": " + jsonString);

    T newModel = deserialize(jsonString, baseClass);
    log("de-serialized " + model.getClass().getSimpleName() + ": " + newModel.toString());

    assertEquals(newModel.toString(), model.toString());
    assertEquals(newModel.getClass().getName(), subClass.getName());
  }

  private Car createCar(String discValue) {
    Car model = new Car.Builder(discValue).make("Ford").bodyStyle("coupe").build();
    return model;
  }

  private Truck createTruck(String discValue) {
    Truck model = new Truck.Builder(discValue).make("Ford").engineType("V8").build();
    return model;
  }

  private AnimalDog createDog(String discValue) {
    AnimalDog model = new AnimalDog();
    model.setAnimalType(discValue);
    model.setBreed("Black Lab");
    model.put("collar_size", "XL");
    return model;
  }

  private AnimalCat createCat(String discValue) {
    AnimalCat model = new AnimalCat();
    model.setAnimalType(discValue);
    model.setColor("brown");
    model.put("collar_size", "S");
    return model;
  }

  private AnimalIguana createIguana(String discValue) {
    AnimalIguana model = new AnimalIguana();
    model.setAnimalType(discValue);
    model.setTailLength(Long.valueOf(10));
    model.put("collar_size", "L");
    return model;
  }

  @Test
  public void testCar() {
    Car model = createCar("Car");
    testSerDeser(model, Vehicle.class, Car.class);
  }

  @Test
  public void testTruck() {
    Truck model = createTruck("truck");
    testSerDeser(model, Vehicle.class, Truck.class);
  }

  // These classes simulate generated model classes that contain a list/map of discriminated oneOf parents.
  public class VehicleHolder {
    int size;
    List<Vehicle> vehicles;

    public VehicleHolder(List<Vehicle> vehicles) {
      this.vehicles = vehicles;
      this.size = vehicles != null ? vehicles.size() : 0;
    }
  }

  public class AnimalHolder {
    int size;
    Map<String, Animal> animals;

    public AnimalHolder(Map<String, Animal> animals) {
      this.animals = animals;
      this.size = animals != null ? animals.size() : 0;
    }
  }

  @Test
  public void testVehicleList() {

    // Create an instance of VehicleHolder that contains a list of Vehicle instances.
    List<Vehicle> vehicleList = new ArrayList<>();
    vehicleList.add(createTruck("truck"));
    vehicleList.add(createCar("Car"));
    VehicleHolder expected = new VehicleHolder(vehicleList);

    // Make sure we can serialize the model instance containing the list of oneOf parents.
    String json = serialize(expected);
    assertNotNull(json);
    log("Vehicle holder (json): " + json);

    VehicleHolder actual = GsonSingleton.getGson().fromJson(json, VehicleHolder.class);
    assertNotNull(actual);
    assertEquals(actual.size, expected.size);
    assertEquals(actual.vehicles, expected.vehicles);
  }

  @Test
  public void testVehiclesNullList() {
    VehicleHolder expected = new VehicleHolder(null);

    String json = serialize(expected);
    assertNotNull(json);
    log("Vehicle holder (json): " + json);

    VehicleHolder actual = GsonSingleton.getGson().fromJson(json, VehicleHolder.class);
    assertNotNull(actual);
    assertEquals(actual.size, expected.size);
    assertEquals(actual.vehicles, expected.vehicles);
  }

  @Test
  public void testVehiclesNullElement() {
    List<Vehicle> vehicles = new ArrayList<>();
    vehicles.add(null);

    VehicleHolder expected = new VehicleHolder(vehicles);

    String json = serialize(expected);
    assertNotNull(json);
    log("Vehicle holder (json): " + json);

    VehicleHolder actual = GsonSingleton.getGson().fromJson(json, VehicleHolder.class);
    assertNotNull(actual);
    assertEquals(actual.size, expected.size);
    assertEquals(actual.vehicles, expected.vehicles);
  }

  @Test
  public void testAnimals() {

    // Create an instance of AnimalHolder that contains a map of Animal instances.
    Map<String, Animal> animals = new HashMap<>();
    animals.put("Fred", createCat("feline"));
    animals.put("Elvis", createDog("dog"));
    animals.put("Tito", createDog("canine"));
    animals.put("Alfred", createIguana("Iguana"));
    AnimalHolder expected = new AnimalHolder(animals);

    String json = serialize(expected);
    assertNotNull(json);
    log("Animal holder (json): " + json);

    AnimalHolder actual = GsonSingleton.getGson().fromJson(json, AnimalHolder.class);
    assertNotNull(actual);
    assertEquals(actual.size, expected.size);
    assertEquals(actual.animals, expected.animals);
  }

  @Test
  public void testAnimalsNullMap() {
    AnimalHolder expected = new AnimalHolder(null);

    String json = serialize(expected);
    assertNotNull(json);
    log("Animal holder (json): " + json);

    AnimalHolder actual = GsonSingleton.getGson().fromJson(json, AnimalHolder.class);
    assertNotNull(actual);
    assertEquals(actual.size, expected.size);
    assertEquals(actual.animals, expected.animals);
  }

  @Test
  public void testAnimalsNullElement() {
    Map<String, Animal> animals = new HashMap<>();
    animals.put("missing_dog", null);
    AnimalHolder expected = new AnimalHolder(animals);

    // We have to enable "serialize nulls" because Gson's handling of maps seems to be inconsistent with their
    // support of lists.
    String json = GsonSingleton.getGsonWithSerializeNulls().toJson(expected);
    assertNotNull(json);
    log("Animal holder (json): " + json);

    AnimalHolder actual = GsonSingleton.getGson().fromJson(json, AnimalHolder.class);
    assertNotNull(actual);
    assertEquals(actual.size, expected.size);
    assertEquals(actual.animals, expected.animals);
  }

  @Test(expectedExceptions = {JsonSyntaxException.class})
  void testTruckDiscPropMissing() {
    Truck model = createTruck(null);
    testSerDeser(model, Vehicle.class, Truck.class);
  }

  @Test(expectedExceptions = {JsonSyntaxException.class})
  void testTruckEmptyDiscValue() {
    Truck model = createTruck("");
    testSerDeser(model, Vehicle.class, Truck.class);
  }

  @Test(expectedExceptions = {JsonSyntaxException.class})
  void testCarBadDiscValue() {
    Car model = createCar("LAMBO");
    testSerDeser(model, Vehicle.class, Car.class);
  }

  @Test
  public void testDog() {
    AnimalDog model = createDog("dog");
    testSerDeser(model, Animal.class, AnimalDog.class);

    model = createDog("canine");
    testSerDeser(model, Animal.class, AnimalDog.class);
  }

  @Test
  public void testCat() {
    AnimalCat model = createCat("feline");
    testSerDeser(model, Animal.class, AnimalCat.class);

    model = createCat("cat");
    testSerDeser(model, Animal.class, AnimalCat.class);
  }

  @Test
  public void testIguana() {
    AnimalIguana model = createIguana("Iguana");
    testSerDeser(model, Animal.class, AnimalIguana.class);
  }

  @Test
  public void testQueryResponse() {
    String json = "{\"aggregations\": [{\"type\": \"term\", \"field\": \"field1\", \"count\": 10}, {\"type\": \"nested\", \"path\": \"prop1/prop2/prop3\" }]}";
    QueryResponse qr = deserialize(json, QueryResponse.class);
    assertNotNull(qr);
    assertNotNull(qr.getAggregations());
    assertEquals(qr.getAggregations().size(), 2);

    QueryAggregation qa = qr.getAggregations().get(0);
    assertTrue(qa instanceof QueryTermAggregation);
    assertEquals(qa.getType(), "term");
    assertEquals(qa.getField(), "field1");
    assertEquals(qa.getCount(), Long.valueOf(10));

    qa = qr.getAggregations().get(1);
    assertTrue(qa instanceof QueryNestedAggregation);
    assertEquals(qa.getType(), "nested");
    assertEquals(qa.getPath(), "prop1/prop2/prop3");
  }
}
