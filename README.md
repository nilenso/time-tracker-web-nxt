# time-tracker-web-nxt

A time tracker application brought to you by ClojureScript with all the goodness
of [re-frame](https://github.com/Day8/re-frame).

This is the front-end to https://github.com/nilenso/time-tracker.

## Installation

### Dependencies

Run `lein deps` to install the project dependencies.

### Configuration

You can add new configuration to/modify existing configuration in `config.cljs`.

## Development Mode

### Setup Emacs:

Put this in your Emacs config file:

```
(defun cider-figwheel-repl ()
  (interactive)
  (save-some-buffers)
  (with-current-buffer (cider-current-repl-buffer)
    (goto-char (point-max))
    (insert "(require 'figwheel-sidecar.repl-api)
             (figwheel-sidecar.repl-api/start-figwheel!) ; idempotent
             (figwheel-sidecar.repl-api/cljs-repl)")
    (cider-repl-return)))
```

Start a Clojure repl from your project with `cider-jack-in` or (`C-c M-j`). Once
ready, start a figwheel repl with `M-x cider-figwheel-repl`.

### Run application:

```
lein do clean, figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Testing

```
lein do clean, doo once (This uses PhantomJS as the default target))
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Deployment

After changing the configuration in `config.cljs` accordingly, run the below script do a production build and deploy the artifacts:

```
./scripts/deploy.sh
```

## License

Copyright © 2017 Nilenso Software LLP

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
