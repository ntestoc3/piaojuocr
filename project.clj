(defproject piaojuocr "0.2.0-SNAPSHOT"
  :description "增加更多ocr识别功能"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :repositories [["vendredi" "https://repository.hellonico.info/repository/hellonico/"]
                 ["spring_plugins" "https://repo.spring.io/plugins-release/"]]
  :dependencies [[org.clojure/clojure "1.10.2"]
                 [com.baidu.aip/java-sdk "4.15.3" :exclusions [org.slf4j/slf4j-api
                                                               org.slf4j/slf4j-simple]] ; baidu ocr sdk
                 ;; [origami "4.1.1-3" :exclusions [opencv/opencv-native
                 ;;                                 hellonico/gorilla-repl]]
                 [org.swinglabs.swingx/swingx-core "1.6.5-1"]
                 [seesaw "1.5.0" :exclusions [org.swinglabs.swingx/swingx-core]] ; swing GUI
                 [camel-snake-kebab/camel-snake-kebab "0.4.2"]
                 [com.taoensso/timbre "5.1.2"] ; logging
                 ;; [org.slf4j/log4j-over-slf4j "1.7.14"]
                 ;; [org.slf4j/jul-to-slf4j "1.7.14"]
                 ;; [org.slf4j/jcl-over-slf4j "1.7.14"]
                 [com.fzakaria/slf4j-timbre "0.3.20"]

                 [com.rpl/specter "1.1.3"] ; dict selector
                 [me.raynes/fs "1.4.6"] ; file util
                 [org.pushing-pixels/radiance-substance "3.5.1"] ;; theme
                 [cheshire "5.10.0"] ;; json

                 ;; https://mvnrepository.com/artifact/net.imagej/ij
                 [net.imagej/ij "1.53b"] ;; image processing

                 ;; uncomment to use only the binary for your platform
                 ;;[opencv/opencv-native "4.0.0-1" :classifier "osx_64"]
                 ;; [opencv/opencv-native "4.1.1-1" :classifier "linux_64"]
                 ;; [opencv/opencv-native "4.1.1-1" :classifier "windows_64"]


                 [cprop/cprop "0.1.17"] ;; env manage
                 ]
  :omit-source true
  :main ^:skip-aot piaojuocr.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
