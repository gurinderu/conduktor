import React from 'react';
import clsx from 'clsx';
import {makeStyles, useTheme} from '@material-ui/core/styles';
import Drawer from '@material-ui/core/Drawer';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Box from '@material-ui/core/Box';
import List from '@material-ui/core/List';
import CssBaseline from '@material-ui/core/CssBaseline';
import Typography from '@material-ui/core/Typography';
import Divider from '@material-ui/core/Divider';
import IconButton from '@material-ui/core/IconButton';
import MenuIcon from '@material-ui/icons/Menu';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import {useQuery} from 'urql';
import {
    useParams,
    useRouteMatch,
    Switch,
    Route,
    Link
} from "react-router-dom"
import ArrowBack from "@material-ui/icons/ArrowBack";
import Topics from "./Topics.js"
import Consumer from "./Consumer.js"

const ClusterQuery = `
 query ($id:ID!){ 
  clusterById(id:$id){
    name
    id
    topics{
      name
    }
  }
}
`;
const drawerWidth = 240;

const useStyles = makeStyles((theme) => ({
    root: {
        backgroundColor: theme.palette.background.default,
        display: 'flex',
    },
    appBar: {
        zIndex: theme.zIndex.drawer + 1,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
    },
    appBarShift: {
        marginLeft: drawerWidth,
        width: `calc(100% - ${drawerWidth}px)`,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    menuButton: {
        marginRight: 36,
    },
    hide: {
        display: 'none',
    },
    drawer: {
        width: drawerWidth,
        flexShrink: 0,
        whiteSpace: 'nowrap',
    },
    drawerOpen: {
        width: drawerWidth,
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    drawerClose: {
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        overflowX: 'hidden',
        width: theme.spacing(7) + 1,
        [theme.breakpoints.up('sm')]: {
            width: theme.spacing(9) + 1,
        },
    },
    secondDrawerOpen: {
        width: 480,
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    secondDrawerClose: {
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        overflowX: 'hidden',
        width: theme.spacing(7) + 1,
        [theme.breakpoints.up('sm')]: {
            width: theme.spacing(9) + 1,
        },
    },
    toolbar: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        padding: theme.spacing(0, 1),
        // necessary for content to be below app bar
        ...theme.mixins.toolbar,
    },
    content: {
        flexGrow: 1,
        padding: theme.spacing(3),
        backgroundColor: theme.palette.background.default,

    },
    fab: {
        position: 'absolute',
        bottom: theme.spacing(2),
        right: theme.spacing(2),
    }
}));

export default function Cluster() {
    let {id} = useParams();
    const classes = useStyles();
    const theme = useTheme();
    const [mainDrawerOpen, setMainDrawerOpen] = React.useState(false);
    const [dialogOpen, setDialogOpen] = React.useState(false);
    const [secondDrawerOpen, setSecondDrawerOpen] = React.useState(false);
    const [selectedTopic, setSelectedTopic] = React.useState(null);

    let {path, url} = useRouteMatch();

    const handleMainDrawerOpen = () => {
        setMainDrawerOpen(true);
    };

    const handleMainDrawerClose = () => {
        setMainDrawerOpen(false);
    };

    const handleSecondaryDrawerClose = () => {
        setSecondDrawerOpen(false);
    };

    const [result, reexecuteQuery] = useQuery({
        query: ClusterQuery,
        variables: {id}
    });

    const handleTopicSelection = (topic) => {
        setSelectedTopic(topic);
        setSecondDrawerOpen(true);
    }

    const {data, fetching, error} = result;

    if (fetching) return <p>Loading...</p>;
    if (error) return <p>Oh no... {error.message}</p>;
    if (!data.clusterById) return <p>Cluster not found</p>;


    return (
        <div className={classes.root}>
            <CssBaseline/>
            <AppBar
                position="fixed"
                className={clsx(classes.appBar, {
                    [classes.appBarShift]: mainDrawerOpen,
                })}
            >
                <Toolbar>
                    <IconButton
                        color="inherit"
                        aria-label="open drawer"
                        onClick={handleMainDrawerOpen}
                        edge="start"
                        className={clsx(classes.menuButton, {
                            [classes.hide]: mainDrawerOpen,
                        })}
                    >
                        <MenuIcon/>
                    </IconButton>
                    <Typography variant="h6" noWrap>
                        {data.clusterById.name}
                    </Typography>
                    <Box display='flex' flexGrow={1}>
                        {/* whatever is on the left side */}
                    </Box>
                    <IconButton edge="end" color="inherit" aria-label="close">
                        <Link to="/">
                            <ArrowBack/>
                        </Link>
                    </IconButton>
                </Toolbar>
            </AppBar>
            <Drawer
                variant="permanent"
                className={clsx(classes.drawer, {
                    [classes.drawerOpen]: mainDrawerOpen,
                    [classes.drawerClose]: !mainDrawerOpen,
                })}
                classes={{
                    paper: clsx({
                        [classes.drawerOpen]: mainDrawerOpen,
                        [classes.drawerClose]: !mainDrawerOpen,
                    }),
                }}
            >
                <div className={classes.toolbar}>
                    <IconButton onClick={handleMainDrawerClose}>
                        {theme.direction === 'rtl' ? <ChevronRightIcon/> : <ChevronLeftIcon/>}
                    </IconButton>
                </div>
                <Divider/>
                <List>
                    <ListItem button>
                        <Link to={`${url}/topics`}>
                            <ListItemText>Topics</ListItemText>
                        </Link>
                    </ListItem>
                </List>
            </Drawer>
            <Drawer
                variant="persistent"
                anchor="right"
                open={secondDrawerOpen}
                classes={{
                    paper: clsx({
                        [classes.secondDrawerOpen]: secondDrawerOpen,
                        [classes.secondDrawerClose]: !secondDrawerOpen,
                    }),
                }}
            >
                <div className={classes.toolbar} style={{paddingTop: '64px', justifyContent: 'flex-start'}}>
                    <IconButton onClick={handleSecondaryDrawerClose}>
                        <ChevronRightIcon/>
                    </IconButton>
                </div>
                <React.Fragment key={selectedTopic}>
                    <Consumer clusterId={id} topic={selectedTopic}/>
                </React.Fragment>
            </Drawer>
            <main className={classes.content}>
                <div className={classes.toolbar}/>
                <Switch>
                    <Route path={`${path}/topics`}>
                        <Topics data={data.clusterById.topics} onTopicSelected={handleTopicSelection}/>
                    </Route>
                </Switch>
            </main>
        </div>
    );
}

