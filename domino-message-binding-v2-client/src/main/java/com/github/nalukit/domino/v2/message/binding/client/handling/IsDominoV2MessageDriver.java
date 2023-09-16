package com.github.nalukit.domino.v2.message.binding.client.handling;

import com.github.nalukit.domino.v2.message.binding.shared.model.IsDominoV2Message;

import java.util.List;

public interface IsDominoV2MessageDriver<P extends IsDominoV2MessageProvider> {

  /**
   * Used to add messags to the MessageFactory and make them visible
   *
   * @param messages messages to display
   */
  void consume(List<? extends IsDominoV2Message> messages);

  /**
   * clears all error messages
   */
  void clearInvalid();

  /**
   * Deregister the driver
   */
  void deregister();

  /**
   * Deregister and destroy the driver
   */
  void deregisterAndDestroy();

  /**
   * Destroy the driver
   */
  void destroy();

  /**
   * Initialize the driver
   *
   * @param messageProvider container with message presentr
   */
  void initialize(P messageProvider);

  /**
   * Register the driver
   */
  void register();

}
