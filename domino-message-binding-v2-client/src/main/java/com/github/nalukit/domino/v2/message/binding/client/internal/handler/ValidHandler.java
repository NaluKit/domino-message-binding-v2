package com.github.nalukit.domino.v2.message.binding.client.internal.handler;

import com.github.nalukit.domino.v2.message.binding.shared.model.IsDominoV2Message;
import elemental2.dom.Element;

import java.util.List;

@FunctionalInterface
public interface ValidHandler {

  void onValid(Element element,
               List<IsDominoV2Message> messages);

}
