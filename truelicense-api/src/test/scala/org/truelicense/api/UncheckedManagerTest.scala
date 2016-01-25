package org.truelicense.api

import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar.mock
import org.truelicense.api.io.{Sink, Source}

@RunWith(classOf[JUnitRunner])
class UncheckedManagerTest extends WordSpec {

  "The unchecked vendor license manager returned by UncheckedManager.from(VendorLicenseManager)" should {
    val checkedManager = mock[VendorLicenseManager]

    val uncheckedManager = UncheckedManager from checkedManager

    "return the checked vendor license manager" in {
      uncheckedManager.checked shouldBe theSameInstanceAs(checkedManager)
    }

    "throw only unchecked exceptions" when {
      "generating license keys" when {
        val checkedGenerator = mock[LicenseKeyGenerator]
        when(checkedManager generator any[License]) thenReturn checkedGenerator

        val uncheckedGenerator = uncheckedManager generator mock[License]

        "calling license()" in {
          when(checkedGenerator.license) thenThrow checkedException
          interceptUncheckedException { uncheckedGenerator.license }
        }

        "calling writeTo(Sink)" in {
          when(checkedGenerator writeTo any[Sink]) thenThrow checkedException
          interceptUncheckedException { uncheckedGenerator writeTo mock[Sink] }
        }
      }
    }
  }

  "The unchecked consumer license manager returned by UncheckedManager.from(ConsumerLicenseManager)" should {
    val checkedManager = mock[ConsumerLicenseManager]

    val uncheckedManager = UncheckedManager from checkedManager

    "return the checked consumer license manager" in {
      uncheckedManager.checked shouldBe theSameInstanceAs(checkedManager)
    }

    "throw only unchecked exceptions" when {
      "installing a license key" in {
        when(checkedManager install any[Source]) thenThrow checkedException
        interceptUncheckedException { uncheckedManager install mock[Source] }
      }

      "viewing a license key" in {
        when(checkedManager view ()) thenThrow checkedException
        interceptUncheckedException { uncheckedManager view () }
      }

      "verifying a license key" in {
        when(checkedManager verify ()) thenThrow checkedException
        interceptUncheckedException { uncheckedManager verify () }
      }

      "uninstalling a license key" in {
        when(checkedManager uninstall ()) thenThrow checkedException
        interceptUncheckedException { uncheckedManager uninstall () }
      }
    }
  }

  private def checkedException = new LicenseManagementException

  private def interceptUncheckedException =
    intercept[UncheckedLicenseManagementException] _
}