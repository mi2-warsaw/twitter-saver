package twitter.tweets

import diode.data.Pot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import twitter.shared.{LoadingView, PaginationView}
import twitter.tweets.TweetsRest.TweetListItem

object TweetsListView {

  case class Props(tweets: Pot[Seq[TweetListItem]],
                   selectedTweet: Option[Int],
                   currentPage: Int,
                   onTweetSelected: Int => Callback,
                   onPageChanged: Int => Callback)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
      val pageSize = 10
      lazy val tweets = p.tweets.get.slice(p.currentPage * pageSize, (p.currentPage + 1) * pageSize)
      <.div(^.className := "card",
        <.div(^.className := "collection with-header",
          <.div(^.className := "collection-header",
            <.h5("Śledzone obiekty")
          ),
          if (p.tweets.nonEmpty) {
            tweets.toTagMod { tweet =>
              val sourceIcon = if (tweet.source == "stream") "autorenew" else "history"
              val typeIcon = if (tweet.objectType == "user") "person" else "description"
              <.a(^.classSet1("collection-item", "active" -> p.selectedTweet.contains(tweet.id)),
                ^.href := "#!", ^.onClick --> p.onTweetSelected(tweet.id),
                if (tweet.deleted) <.span(^.className := "new badge red", vdomAttrVtString("data-badge-caption", ""), "Usunięto") else EmptyVdom,
                <.i(^.className := "material-icons left", sourceIcon),
                <.i(^.className := "material-icons left", typeIcon),
                tweet.query
              )
            }
          } else if (p.tweets.isPending) {
            <.div(^.className := "center loading", LoadingView())
          } else {
            EmptyVdom
          },
          p.tweets.exceptionOption.whenDefined { e =>
            <.div(^.className := "center loading red-text", e.getMessage)
          },
          if (p.tweets.nonEmpty && tweets.nonEmpty) {
            <.div(
              PaginationView(PaginationView.Props(
                itemsCount = p.tweets.get.size,
                currentPage = p.currentPage,
                pageSize = pageSize,
                onPageChange = p.onPageChanged
              ))
            )
          } else {
            EmptyVdom
          }
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("TweetsListView")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)
}
