package io.github.nalukit.domino.v2.message.binding.client.handling;

import io.github.nalukit.domino.v2.message.binding.client.internal.helper.DominoV2MessageElementWrapper;
import io.github.nalukit.domino.v2.message.binding.shared.model.IsDominoV2Message;
import org.dominokit.domino.ui.events.EventType;
import org.dominokit.domino.ui.forms.InputFormField;
import org.dominokit.domino.ui.forms.suggest.AbstractSelect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract class of the message driver
 *
 * Contains the base implementation of the driver
 *
 * @param <P> name of the MessageProvider
 */
public abstract class AbstractDominoV2MessageDriver<P extends IsDominoV2MessageProvider>
    implements IsDominoV2MessageDriver<P> {

  protected boolean clearOnBlur;

  protected Map<String, DominoV2MessageElementWrapper> messageElementWrappers;

  public AbstractDominoV2MessageDriver() {
    this.messageElementWrappers = new HashMap<>();
    this.clearOnBlur = true;
  }

  @Override
  public void deregister() {
    this.messageElementWrappers.values()
                               .forEach(e -> {
                                 if (e.getFormElement() instanceof InputFormField) {
                                   ((InputFormField<?, ?, ?>) e.getFormElement())
                                       .getInputElement()
                                       .removeEventListener(EventType.blur,
                                                            e.getBlurEventListener());
                                   e.setBlurEventListener(null);
                                 } else if (e.getFormElement() instanceof AbstractSelect) {
                                   ((AbstractSelect<?, ?, ?, ?, ?>) e.getFormElement())
                                       .getInputElement()
                                       .removeEventListener(EventType.blur,
                                                            e.getBlurEventListener());
                                   e.setBlurEventListener(null);
                                 }
                                });
  }

  @Override
  public void clearInvalid() {
    this.messageElementWrappers.values()
                               .forEach(w -> w.getFormElement()
                                              .clearInvalid());
  }

  @Override
  public void consume(List<? extends IsDominoV2Message> messages) {
    List<IsDominoV2Message> unconsumedMessages = new ArrayList<>();
    messages.forEach(m -> {
//      GWT.log(IsDominoMessage.Target.FIELD.toString());
//      GWT.log(m.getTarget()
//               .toString());
      if (IsDominoV2Message.Target.FIELD.toString()
                                        .equals(m.getTarget()
                                               .toString())) {
        m.getErrorSources()
         .stream()
         .map(errorSource -> this.messageElementWrappers.get(errorSource))
         .forEach(wrapper -> {
           if (Objects.isNull(wrapper)) {
             unconsumedMessages.add(m);
           }
           wrapper.getFormElement()
                  .invalidate(m.getText());
         });
      } else {
        unconsumedMessages.add(m);
      }

    });
    //    // TODO handle unconsumed messages
  }

  @Override
  public void deregisterAndDestroy() {
    this.deregister();
    this.destroy();
  }

  @Override
  public void destroy() {
    this.messageElementWrappers.clear();
  }

  @Override
  public void register() {
    this.messageElementWrappers.values()
                               .forEach(w -> {
                                 if (clearOnBlur) {
                                   elemental2.dom.EventListener eventlistener = evt -> w.getFormElement()
                                                                                      .clearInvalid();
                                   if (w.getFormElement() instanceof InputFormField) {
                                     ((InputFormField<?, ?, ?>) w.getFormElement())
                                      .getInputElement()
                                      .addEventListener(EventType.blur,
                                                        eventlistener);
                                   } else if (w.getFormElement() instanceof AbstractSelect) {
                                     ((AbstractSelect<?, ?, ?, ?, ?>) w.getFormElement())
                                         .getInputElement()
                                         .addEventListener(EventType.blur,
                                                           eventlistener);
                                   }
                                   w.setBlurEventListener(eventlistener);
                                 }
                               });


  }

}
