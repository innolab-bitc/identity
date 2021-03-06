/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.mifos.anubis.api.v1.client.Anubis;
import io.mifos.anubis.api.v1.domain.ApplicationSignatureSet;
import io.mifos.anubis.api.v1.domain.Signature;
import io.mifos.core.api.context.AutoSeshat;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.api.util.NotFoundException;
import io.mifos.identity.api.v1.domain.Authentication;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Myrle Krantz
 */
public class TestKeyRotation extends AbstractComponentTest {
  @Test
  public void testKeyRotation() throws InterruptedException {
    //TODO: Asserts on canAccessResources are commented out below because spring cashes credentials.
    //Once a successful authentication has occurred with in anubis, that user is not reauthenticated across the same
    //connection, even if the token changes, or if the token is invalidated.  This is a problem we need to find a
    //solution for.  Simply calling SecurityContextHolder.clearContext is insufficient because the event which leads
    //to this necessity may have been handled by a separate service.  We'd need to make that call across on threads
    //in all services.
    //
    //To see the offending code, look in the functions principalChanged and requiresAuthentication in
    // org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
    final Anubis anubis = tenantApplicationSecurityEnvironment.getAnubis();

    //noinspection EmptyTryBlock
    try (final AutoUserContext ignored = enableAndLoginAdmin())
    {
      //Don't do anything yet.
    }

    final String systemToken = tenantApplicationSecurityEnvironment.getSystemSecurityEnvironment().systemToken(APP_NAME);

    try (final AutoSeshat ignored1 = new AutoSeshat(systemToken)) {
      //Create a signature set then test that it is listed.
      final String timestamp = getTestSubject().createSignatureSet().getTimestamp();
      {
        final List<String> signatureSets = anubis.getAllSignatureSets();
        Assert.assertTrue(signatureSets.contains(timestamp));
      }


      final Authentication adminAuthenticationOnFirstKeyset =
              getTestSubject().login(ADMIN_IDENTIFIER, Helpers.encodePassword(ADMIN_PASSWORD));

      //TODO: Assert.assertTrue(canAccessResources(adminAuthenticationOnFirstKeyset));

      //For identity, application signature and identity manager signature should be identical.
      final ApplicationSignatureSet signatureSet = anubis.getSignatureSet(timestamp);
      Assert.assertEquals(signatureSet.getApplicationSignature(), signatureSet.getIdentityManagerSignature());

      final Signature applicationSignature = anubis.getApplicationSignature(timestamp);
      Assert.assertEquals(signatureSet.getApplicationSignature(), applicationSignature);

      TimeUnit.SECONDS.sleep(2); //Timestamp has resolution at seconds level -- Make sure that second signature set has different timestamp from the first one.

      //Create a second signature set and test that it and the previous signature set are listed.
      final String timestamp2 = getTestSubject().createSignatureSet().getTimestamp();
      {
        final List<String> signatureSets = anubis.getAllSignatureSets();
        Assert.assertTrue(signatureSets.contains(timestamp));
        Assert.assertTrue(signatureSets.contains(timestamp2));
      }

      final Authentication adminAuthenticationOnSecondKeyset =
              getTestSubject().login(ADMIN_IDENTIFIER, Helpers.encodePassword(ADMIN_PASSWORD));

      //TODO: Assert.assertTrue(canAccessResources(adminAuthenticationOnFirstKeyset));
      //TODO: Assert.assertTrue(canAccessResources(adminAuthenticationOnSecondKeyset));

      //Get the newly created signature set, and test that its contents are correct.
      final ApplicationSignatureSet signatureSet2 = anubis.getSignatureSet(timestamp2);
      Assert.assertEquals(signatureSet2.getApplicationSignature(), signatureSet2.getIdentityManagerSignature());

      //Delete one of the signature sets and test that it is no longer listed.
      anubis.deleteSignatureSet(timestamp);
      {
        final List<String> signatureSets = anubis.getAllSignatureSets();
        Assert.assertFalse(signatureSets.contains(timestamp));
        Assert.assertTrue(signatureSets.contains(timestamp2));
      }

      //TODO: Assert.assertTrue(canAccessResources(adminAuthenticationOnSecondKeyset));
      Assert.assertFalse(canAccessResources(adminAuthenticationOnFirstKeyset));

      //Getting the newly deleted signature set should fail.
      try {
        anubis.getSignatureSet(timestamp);
        Assert.fail("Not found exception should be thrown.");
      } catch (final NotFoundException ignored) {
      }

      //Getting the newly deleted application signature set should likewise fail.
      try {
        anubis.getApplicationSignature(timestamp);
        Assert.fail("Not found exception should be thrown.");
      } catch (final NotFoundException ignored) {
      }
    }
  }

  private boolean canAccessResources(Authentication adminAuthentication) {
    try (final AutoUserContext ignored = new AutoUserContext(ADMIN_IDENTIFIER, adminAuthentication.getAccessToken())) {
      getTestSubject().getUsers();
      return true;
    }
    catch (Throwable ignored) {
      return false;
    }
  }
}
