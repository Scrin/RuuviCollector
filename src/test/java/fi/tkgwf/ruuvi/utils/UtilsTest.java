package fi.tkgwf.ruuvi.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

   @Test
   void hasMacAddress() {
      assertTrue(Utils.hasMacAddress("> 04 3E 21 02 01 03 01 EF C0 45 EB B7 C9 15 02 01 06 11 FF 99"));
      assertFalse(Utils.hasMacAddress("04 03 6B 0E 26 C8 6C FF CE 00 04 03 EB 0B EF 9E"));
   }

   @Test
   void getMacFromLine() {

      // Valid Input
      assertEquals("C9B7EB45C0EF", Utils.getMacFromLine("> 04 3E 21 02 01 03 01 EF C0 45 EB B7 C9 15 02 01 06 11 FF 99"));
      
      // Check short line with mac
      assertEquals("C9B7EB45C0EF", Utils.getMacFromLine("> 04 3E 21 02 01 03 01 EF C0 45 EB B7 C9"));
      
      // Check null safe
      assertNull(Utils.getMacFromLine(null));

      // Check short line
      assertNull(Utils.getMacFromLine("> 04 3E 21 02 01 03 01 EF C0 45 EB B7"));

      // Check short line - with trailling whitespace
      assertNull(Utils.getMacFromLine("> 04 3E 21 02 01 03 01 EF C0 45 EB B7        "));
      
      // Check second line
      assertNull(Utils.getMacFromLine("04 3E 21 02 01 03 01 EF C0 45 EB B7 C9 15 02 01 06 11 FF 99"));
   }
}
