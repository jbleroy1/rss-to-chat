package com.cloud.google;


import com.cloud.google.config.Configuration;
import com.cloud.google.config.MessageData;
import com.google.api.services.chat.v1.model.CardWithId;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1Button;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1ButtonList;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1Card;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1CardHeader;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1OnClick;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1OpenLink;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1Section;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1TextParagraph;
import com.google.api.services.chat.v1.model.GoogleAppsCardV1Widget;
import com.google.api.services.chat.v1.model.Message;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ProductFeed {

  public List<MessageData> getMessages() {
    return messages.stream().map(message -> new MessageData(message, this))
        .collect(Collectors.toList());
  }

  private SyndFeed syndFeed;
  private Configuration product;

  private List<Message> messages;

  public ProductFeed(Configuration configuration) {

    this.product = configuration;
    try {
      URL feedSource = new URL(configuration.getURL());
      SyndFeedInput input = new SyndFeedInput();
      syndFeed = input.build(new XmlReader(feedSource));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public SyndFeed getSyndFeed() {
    return syndFeed;
  }

  public void createCards() {
    messages = this.syndFeed.getEntries().stream()
        .filter(
            syndEntry -> syndEntry.getUpdatedDate().getTime() == this.syndFeed.getPublishedDate()
                .getTime())
        .map(syndEntry -> this.createCard(syndEntry)).flatMap(messages -> messages.stream())
        .collect(
            Collectors.toList());

  }

  private Message createCardFromElements(Elements elements, SyndEntry syndEntry) {
    CardWithId cardId = new CardWithId();
    cardId.setCardId(this.syndFeed.getTitle());
    GoogleAppsCardV1Card cardV1 = new GoogleAppsCardV1Card();
    cardV1.setName(this.syndFeed.getTitle());

    GoogleAppsCardV1CardHeader cardHeader = new GoogleAppsCardV1CardHeader();
    cardHeader.setTitle(this.syndFeed.getTitle())
        .setSubtitle("Release Note Bot")
        .setImageType("SQUARE")
        .setImageUrl(this.product.getIconURL())
        .setImageAltText("Default Avatar");
    cardV1.setHeader(cardHeader);
    GoogleAppsCardV1Section section = new GoogleAppsCardV1Section();

    int i = 0;
    String header = "";
    cardHeader.setTitle(elements.get(0).html());
    while (elements.get(i).tag().getName().equals("h3") || elements.get(i)
        .hasClass("release-note-product-title")) {
      header += elements.get(i).html() + "\n";
      i++;
    }
    section.setHeader(header);
    List<Element> subList = elements.subList(i, elements.size());
    String paragraph = subList.stream().map(elements1 -> elements1.html())
        .collect((Collectors.joining("")));
    GoogleAppsCardV1Widget widget = new GoogleAppsCardV1Widget();
    GoogleAppsCardV1Widget widgetButton = new GoogleAppsCardV1Widget();
    List<GoogleAppsCardV1TextParagraph> paragraphs = Arrays.asList(
        new GoogleAppsCardV1TextParagraph().setText(paragraph));
    GoogleAppsCardV1Button button = new GoogleAppsCardV1Button();
    GoogleAppsCardV1OnClick onClick = new GoogleAppsCardV1OnClick();
    GoogleAppsCardV1OpenLink openLink = new GoogleAppsCardV1OpenLink();
    openLink.setUrl(syndEntry.getLink());
    onClick.setOpenLink(openLink);
    button.setText("Read the release note").setOnClick(onClick);
    GoogleAppsCardV1ButtonList buttonList = new GoogleAppsCardV1ButtonList();
    buttonList.setButtons(Arrays.asList(button));
    widget.setTextParagraph(paragraphs.get(0));
    widgetButton.setButtonList(buttonList);
    section.setWidgets(Arrays.asList(widget, widgetButton));
    cardV1.setSections(Arrays.asList(section));
    cardId.setCard(cardV1);

    return new Message().setCardsV2(Arrays.asList(cardId));
  }

  private List<Message> createCard(SyndEntry syndEntry) {

    List<Elements> splitH3 = splitH3(syndEntry);
    return splitH3.stream().map(elements -> createCardFromElements(elements, syndEntry))
        .collect(Collectors.toList());
  }


  private List<Elements> splitH3(SyndEntry syndEntry) {
    Document document = Jsoup.parse(syndEntry.getContents().get(0).getValue());
    List<Elements> elementsList = new ArrayList<>();
    Elements currentElements = new Elements();
    Elements elementSiblings = document.getElementsByTag("body").get(0).getAllElements().get(1)
        .nextElementSiblings();
    currentElements.add(document.getElementsByTag("body").get(0).getAllElements().get(1));
    currentElements.add(elementSiblings.get(0));
    for (int i = 1; i < elementSiblings.size(); i++) {
      Element el = elementSiblings.get(i);
      if ((!el.tag().getName().equals("h3") && !el.hasClass("release-note-product-title"))
          || isElementsHeaderOnly(currentElements)) {
        currentElements.add(el);
      } else {
        elementsList.add(currentElements);
        currentElements = new Elements();
        currentElements.add(el);
      }

    }
    elementsList.add(currentElements);
    return elementsList;
  }

  public boolean isElementsHeaderOnly(Elements elements) {
    return !elements.stream().filter(
            element -> !element.hasClass("release-note-product-title") && !element.tag().getName()
                .equals("h3")).findFirst()
        .isPresent();
  }

  public Configuration getProduct() {
    return product;
  }
}
