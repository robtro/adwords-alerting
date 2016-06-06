// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.ads.adwords.awalerting.util;

import com.google.api.ads.adwords.awalerting.AlertProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

/**
 * Utility class for retrying operations.
 *
 * @param <V> return value of the closure that is being run with retires.
 */
public class RetryHelper<V> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RetryHelper.class);

  private final Callable<V> callable;
  private final String actionDescription; // Short description of the action.
  private final int maxNumberOfAttempts; // Max number of attempts.
  private final int backoffInterval; // The backoff interval in microseconds.

  // List of exceptions that break the retry.
  @Nullable private final ImmutableList<Class<? extends Exception>> nonRetriableExceptions;

  private RetryHelper(
      Callable<V> callable,
      String actionDescription,
      int maxNumberOfAttempts,
      int backoffInterval,
      ImmutableList<Class<? extends Exception>> nonRetriableExceptions) {
    this.callable = Preconditions.checkNotNull(callable);
    this.actionDescription = Preconditions.checkNotNull(actionDescription);

    Preconditions.checkArgument(maxNumberOfAttempts > 0);
    this.maxNumberOfAttempts = maxNumberOfAttempts;
    this.backoffInterval = backoffInterval;
    this.nonRetriableExceptions = nonRetriableExceptions;
  }

  /**
   * Checks the cause of AlertProcessingException against non-retriable exception list, to decide
   * whether it's retriable.
   *
   * @param e the AlertProcessingException being caught
   * @return whether it's retriable.
   */
  private boolean shouldRetry(AlertProcessingException e) {
    if (nonRetriableExceptions != null) {
      Class<?> causeClass = e.getCause().getClass();

      for (Class<? extends Exception> nonRetriableException : nonRetriableExceptions) {
        if (nonRetriableException.isAssignableFrom(causeClass)) {
          return false;
        }
      }
    }

    return true;
  }

  private V callWithRetries() throws AlertProcessingException {
    V result = null;
    AlertProcessingException lastException = null;

    for (int i = 1; i <= maxNumberOfAttempts; ++i) {
      try {
        lastException = null;
        result = callable.call();
        break;
      } catch (AlertProcessingException e) {
        lastException = e;

        if (shouldRetry(e)) {
          LOGGER.error("Failed to {}, attempt #{}/{}.", actionDescription, i, maxNumberOfAttempts);
        } else {
          LOGGER.error(
              "Failed to {} and encountered non-retriable {}, skip retry!",
              actionDescription, e.getCause().getClass().getName());
          throw e;
        }
      } catch (Exception e) {
        String errorMsg = 
            String.format(
                "Failed to %s and encountered unexpected %s, skip retry!",
                actionDescription, e.getClass().getName());
        LOGGER.error(errorMsg);
        throw new AlertProcessingException(errorMsg, e);
      }

      // If we haven't succeeded, slow down the rate of requests (with exponential back off).
      try {
        // Sleep unless this was the last attempt.
        if (i < maxNumberOfAttempts) {
          long backoff = (long) Math.scalb(this.backoffInterval, i);
          LOGGER.info("Back off for {}ms before next retry.", backoff);
          Thread.sleep(backoff);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AlertProcessingException(
            "InterruptedException occurs while waiting to " + actionDescription, e);
      }
    }

    if (result == null) {
      throw new AlertProcessingException(
          "Failed to " + actionDescription + " after all retries.", lastException);
    }

    return result;
  }
  
  /**
   * Runs the callable and possibly retries with exponential backoff.
   *
   * @param callable the callable to run
   * @param actionDescription short description of the callble's action
   * @param maxNumberOfAttempts max number of attempts
   * @param backoffInterval the exponential backoff interval between retires (in microseconds)
   * @param nonRetriableExceptions the list of exceptions that won't be retried
   * @return the returned result of the callable
   * @throws AlertProcessingException when some non-retriable exception occurs, some exception
   * other than AlertProcessingException occurs, the thread is interrupted during waiting, or all
   * retries are exhausted.
   */
  public static <V> V callsWithRetries(
      Callable<V> callable,
      String actionDescription,
      int maxNumberOfAttempts,
      int backoffInterval,
      ImmutableList<Class<? extends Exception>> nonRetriableExceptions)
      throws AlertProcessingException {
    RetryHelper<V> retryHelper = new RetryHelper<V>(
        callable, actionDescription, maxNumberOfAttempts, backoffInterval, nonRetriableExceptions);
    return retryHelper.callWithRetries();
  }
}
