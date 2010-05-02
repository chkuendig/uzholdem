package com.biotools.util;

import java.security.SecureRandom;

public class Randomizer extends SecureRandom
{
  private static Randomizer staticRand = null;

  public static synchronized Randomizer getRandomizer()
  {
    if (staticRand == null) {
      staticRand = new Randomizer();
    }
    return staticRand;
  }

  public int randInt(int range)
  {
    return ((int)(nextDouble() * range) % range);
  }

  public double nextDouble() {
    return super.nextDouble();
  }
}