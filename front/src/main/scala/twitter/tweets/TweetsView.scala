package twitter.tweets

import diode.Action
import diode.data.PotState.PotEmpty
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import twitter.shared.LoadingView

object TweetsView {

  case class Props(tweetsModel: TweetsModel, dispatch: Action => Callback)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
        <.div(^.className := "row",
          <.div(^.className := "col s6",
            AddObjectView(AddObjectView.Props(
              info = p.tweetsModel.newObjectInfo,
              progress = p.tweetsModel.newObjectProgress,
              error = p.tweetsModel.newObjectError,
              onQueryChange = x => p.dispatch(ObjectQueryChanged(x)),
              onTypeChange = x => p.dispatch(ObjectTypeChanged(x)),
              onSourceChange = x => p.dispatch(ObjectSourceChanged(x)),
              onSubmit = p.dispatch(ObjectSubmit)
            )),
            TweetsListView(TweetsListView.Props(
              tweets = p.tweetsModel.tweets,
              selectedTweet = p.tweetsModel.selectedObjectId,
              currentPage = p.tweetsModel.listCurrentPage,
              onTweetSelected = id => p.dispatch(SelectObject(id)),
              onPageChanged = page => p.dispatch(ListPageChanged(page))
            ))
          ),
          <.div(^.className := "col s6",
            if (p.tweetsModel.objectStats.state == PotEmpty) {
              EmptyVdom
            } else {
              ObjectView(ObjectView.Props(
                objectStats = p.tweetsModel.objectStats,
                from = p.tweetsModel.objectStatsFrom,
                to = p.tweetsModel.objectStatsTo,
                currentPage = p.tweetsModel.statsCurrentPage,
                onFromChange = _ => Callback.empty,
                onToChange = _ => Callback.empty,
                onPageChange = page => p.dispatch(StatsPageChanged(page))
              ))
            }
          ),
          ObjectDeleteView(ObjectDeleteView.Props(
            query = p.tweetsModel.objectStats.toOption.map(_.query).getOrElse(""),
            isDeleted = p.tweetsModel.objectStats.toOption.exists(_.deleted),
            deleteCheck = p.tweetsModel.objectDeleteAll,
            onDeleteCheck = x => p.dispatch(ObjectDeleteAllChanged(x)),
            onSubmit = p.dispatch(ObjectDelete(p.tweetsModel.selectedObjectId.getOrElse(0)))
          ))
        )
    }
  }

  val component = ScalaComponent.builder[Props]("TweetsView")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(tweetsModel: TweetsModel, dispatch: Action => Callback) =
    component(Props(tweetsModel, dispatch))
}
