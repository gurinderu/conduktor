import React, {useRef} from 'react';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import Divider from '@material-ui/core/Divider';
import Toolbar from '@material-ui/core/Toolbar';
import Button from '@material-ui/core/Button';
import Box from '@material-ui/core/Box';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';
import InputLabel from '@material-ui/core/InputLabel';
import FormControl from '@material-ui/core/FormControl';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import {useSubscription} from 'urql';

import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles((theme) => ({
    menuButton: {
        marginRight: 36,
    },
    formControl: {
        flex: 1,
        minWidth: 450,
        margin: '0 15px 0 15px',
    },
}))

const ConsumeMessages = `
 subscription(
  $clusterId: ID!
  $topic: String!
  $keyFormat: Format!
  $valueFormat: Format!
  $offset: Offset!
) {
  consumer(
    clusterId: $clusterId
    topicName: $topic
    keyFormat: $keyFormat
    valueFormat: $valueFormat
    offset: $offset
  ) {
    time
    key
    value
  }
}
`;


export default function Consumer({clusterId, topic}) {
    const classes = useStyles();

    const [keyFormatValue, setKeyFormatValue] = React.useState('String');
    const [valueFormatValue, setValueFormatValue] = React.useState('String');
    const [offsetValue, setOffsetFormatValue] = React.useState('Latest');
    const [pauseValue, setPauseValue] = React.useState(true);

    const handleSubscription = (newMessages = [], response) => {
        return [response.consumer, ...newMessages];
    };

    const [res] = useSubscription({
        query: ConsumeMessages,
        variables: {
            clusterId: clusterId,
            topic: topic,
            keyFormat: keyFormatValue,
            valueFormat: valueFormatValue,
            offset: offsetValue
        },
        pause: pauseValue
    }, handleSubscription);

    const handleConsumeData = () => {
        setPauseValue(false);
    }

    const handleStop = () => {
        setPauseValue(true);
    }


    return (
        <React.Fragment>
            <Toolbar>
                <Typography variant="h6" gutterBottom>
                    Topic: {topic}
                </Typography>
                <Box display='flex' flexGrow={1}/>
                {pauseValue ?
                    <Button color="primary" variant="contained" onClick={handleConsumeData}>Consume Data</Button>
                    :
                    <Button color="primary" variant="contained" onClick={handleStop}>Stop</Button>
                }
            </Toolbar>
            <Divider/>
            <Grid container spacing={3}>
                <Grid item xs={12}>
                    <FormControl className={classes.formControl}>
                        <InputLabel>Key</InputLabel>
                        <Select
                            id="key"
                            label="key"
                            fullWidth
                            value={keyFormatValue}
                            onChange={(event) => setKeyFormatValue(event.target.value)}

                        >
                            <MenuItem value='String'>String</MenuItem>
                            <MenuItem value='JSON'>JSON</MenuItem>
                        </Select>
                    </FormControl>
                </Grid>
                <Grid item xs={12}>
                    <FormControl className={classes.formControl}>
                        <InputLabel>Value</InputLabel>
                        <Select
                            id="value"
                            label="value"
                            fullWidth
                            displayEmpty
                            value={valueFormatValue}
                            onChange={(event) => setValueFormatValue(event.target.value)}

                        >
                            <MenuItem value='String'>String</MenuItem>
                            <MenuItem value='JSON'>JSON</MenuItem>
                        </Select>
                    </FormControl>
                </Grid>
                <Grid item xs={12}>
                    <FormControl className={classes.formControl}>
                        <InputLabel>Start from</InputLabel>
                        <Select
                            id="offset"
                            label="offset"
                            fullWidth
                            value={offsetValue}
                            onChange={(event) => setOffsetFormatValue(event.target.value)}
                        >
                            <MenuItem value='Earliest'>Earliest</MenuItem>
                            <MenuItem value='Latest'>Latest</MenuItem>
                        </Select>
                    </FormControl>
                </Grid>
                <Grid item xs={12}>
                    {
                        !res.data ? <Typography align={"center"} variant="h6" gutterBottom>
                                No new messages
                            </Typography> :
                            <List dense={true}>
                                {res.data.map(message => (
                                    <ListItem>
                                        <Box>
                                            <ListItemText primary={"Time: " + message.time}/>
                                            <ListItemText primary={"Key: " + message.key}/>
                                            <ListItemText primary={"Value: " + message.value}/>
                                        </Box>
                                    </ListItem>
                                ))}
                            </List>
                    }

                </Grid>
            </Grid>
        </React.Fragment>
    );
}
