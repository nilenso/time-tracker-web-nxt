## Refactoring Goals

- Move data computation to subscriptions and keep views simple
- Move non-trivial event handlers out to seperate functions to make it
  easier to test them.
- Write more specs where necessary
- See if any other global interceptors could help simplify code
- Organize code in different namespaces when working with different views
  - https://github.com/Day8/re-frame/blob/master/docs/Basic-App-Structure.md#larger-apps
  - Use namespaced ids for events/subscriptions if needed
- Move input signals from parent to child component where necessary
- Perhaps move creation of websocket into a subscription?
  - https://github.com/Day8/re-frame/blob/master/docs/Subscribing-To-External-Data.md
- Create a replacement for reg-event-db using standard interceptors for ease of use
  - https://github.com/Day8/re-frame/blob/master/docs/Debugging-Event-Handlers.md#too-much-repetition---part-2
- Have seperate keys for seperate views in re-frame.db/db?
  - Makes it obvious as to what view uses what data
- Eliminate side causes (implicit inputs) from handlers to make them pure
  - Easier to test this
  - Use `inject-cofx` to inject required inputs for handlers
- Use the `trim-v` interceptor to make event handlers more readable
- Use [reframe-websocket](https://github.com/ftravers/reframe-websocket) instead of
  handrolled implementation perhaps?

## Resources

- https://groups.google.com/forum/#!topic/clojurescript/Ta8HnekpiZk
