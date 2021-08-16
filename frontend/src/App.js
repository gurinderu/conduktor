import './App.css';
import {createClient, Provider} from 'urql';
import Cluster from './Cluster.js'
import Clusters from './Clusters.js'
import {
    BrowserRouter as Router,
    Switch,
    Route,
    Link
} from "react-router-dom";
import { defaultExchanges, subscriptionExchange } from 'urql';
import { SubscriptionClient } from 'subscriptions-transport-ws';

const subscriptionClient = new SubscriptionClient('ws://localhost:8080/ws/graphql', { reconnect: true });

const client = createClient({
    url: 'http://localhost:8080/api/graphql',
    exchanges: [
        ...defaultExchanges,
        subscriptionExchange({
            forwardSubscription: (operation) => subscriptionClient.request(operation)
        }),
    ],
});


function App() {
    return (
        <Provider value={client}>
            <Router>
                <Switch>
                    <Route exact path="/">
                        <Clusters/>
                    </Route>
                    <Route path="/cluster/:id">
                        <Cluster/>
                    </Route>
                </Switch>
            </Router>
        </Provider>
    );
}

export default App;
