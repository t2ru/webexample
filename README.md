# webexample

Webサービスの実装例

## 使用ライブラリ

* [サーバ]プラットフォーム: Immutant
* [サーバ]パスのルーティング: Compojure
* [サーバ]REST応答の生成: Liberator
* [サーバ]開発用DBMS: H2
* [クライアント]DOM生成・更新: Reagent
* [クライアント]クエリ: cljs-ajax

## Usage

1. lein cljsbuild onceでclojurescriptをコンパイル。
2. mkdir db でお試しDB用ディレクトリを作成。
1. replを起動。
2. webexample.coreをロード。
3. webexample.core/make-dbでテーブル作成。
4. webexample.core/run を実行してサーバ起動。


## 設計のポイント

### サーバ

* RESTのロジックはLiberatorを使い、自前では書かない。
  - 標準的な枠組みを利用することで、
    プログラマのHTTPに対する理解不足によるバグを防ぐ。
* サービスで呼び出される全SQLをハンドラのトップレベルに書く。
  - 非効率性は書いている間に明らかになるので、性能問題が起きない。
  - 起きても生SQLなので、すぐにチューニングできる。
* 全部XAのトランザクションで囲う。
  - DBだけでなく、メッセージングも含めたトランザクション制御をする。
  - resourceはcompojureのハンドラ内では実行されないので、
    ringのmiddlewareにする。
  - (ただし、XAの信じ過ぎは良くない。)
* Immutantで作成。
  - warにしてJBossのコンテナに載せれば、そのまま即商用。


### クライアント

* Reagentでatomを更新したら関連するDOMが自動的に書き換わる。 cool!
* Ajaxは普通にcljs-ajaxが一番簡単。


## License

Copyright c 2015 Tetsuya Takatsuru

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
