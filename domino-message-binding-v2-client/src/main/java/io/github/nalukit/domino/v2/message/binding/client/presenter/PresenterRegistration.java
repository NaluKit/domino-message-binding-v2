package io.github.nalukit.domino.v2.message.binding.client.presenter;

/**
 * Registration of a presenter for the MessageFactory
 */
public interface PresenterRegistration {

  /**
   * removes the registered presenter from the MessageFactory
   */
  void remove();

}
