package com.github.nalukit.domino.message.binding.client.internal.handler;

import com.github.nalukit.domino.message.binding.shared.model.IsDominoMessage;
import elemental2.dom.Element;

import java.util.List;

@FunctionalInterface
public interface InvalidHandler {

  void onInvalid(Element element,
                 List<IsDominoMessage> messages);

}

