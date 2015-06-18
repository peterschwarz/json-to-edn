# JSON <-> EDN Converter

Written in Clojurescript for [Clojure.MN](http://clojure.mn).

See it in action [here](https://peterschwarz.github.io/json-to-edn)

## Building

For Development

Fire up figwheel

```
> rlwrap lein figwheel dev test
```

and navigate to [localhost:3449](http://localhost:3449) to see the page.

To run the unit tests, navigate to the [test runner](http://localhost:3449/test.html).  This will auto-run the tests when files change, with the results in the Javascript console.  If you add a `deftest`, you'll need to at least touch `test/json_to_edn/test_runner.clj` so it is included in the tests.  If you add a new test namespace, you'll need to add references to it `test_runner.clj` as well.


For deployment: 

```
> lein clean
> lein cljsbuild once min
```
