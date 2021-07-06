package com.ibm.cloud.sdk.core.util.discriminator;

import com.google.gson.Gson;
import com.ibm.cloud.sdk.core.util.GsonSingleton;
import org.junit.Before;
import org.junit.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class DiscriminatorBasedTypeAdapterFactoryTest {

  private Gson gson;

  @Before
  public void before() {
    gson = GsonSingleton.getGsonWithoutPrettyPrinting();
  }

  @Test
  public void deserializeShouldProduceTheProperObjectType() {

    String resolveJson = "{\"action\":\"resolve\"}";
    Object deserializeToObjectType = gson.fromJson(resolveJson, StatusPayload.class);
    assertEquals(deserializeToObjectType.getClass().getName(), ResolvePayload.class.getName());

    ResolvePayload deserializeToExactType = (ResolvePayload) gson.fromJson(resolveJson, StatusPayload.class);
    assertEquals(deserializeToExactType.action, "resolve");
  }

  @Test
  public void deserializeWithoutDiscriminatorPropertyNameShouldResultTheSuppliedType() {
    String json = "{\"action\":\"accept\"}";
    Object result = gson.fromJson(json, StatusPayloadWithoutDiscriminatorPropertyName.class);
    assertEquals(result.getClass().getName(), StatusPayloadWithoutDiscriminatorPropertyName.class.getName());
    assertNotEquals(result.getClass().getName(), AcceptPayloadWithoutDiscriminatorPropertyName.class.getName());
  }

  @Test
  public void deserializedWithoutDiscriminatorMappingShouldResultTheSuppliedType() {
    String json = "{\"action\":\"accept\"}";
    Object result = gson.fromJson(json, StatusPayloadWithoutDiscriminatorMapping.class);
    assertEquals(result.getClass().getName(), StatusPayloadWithoutDiscriminatorMapping.class.getName());
    assertNotEquals(result.getClass().getName(), AcceptPayloadWithoutDiscriminatorMapping.class.getName());
  }

}
