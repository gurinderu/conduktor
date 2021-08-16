import React from 'react';
import clsx from 'clsx';
import {makeStyles, useTheme} from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import List from '@material-ui/core/List';
import CssBaseline from '@material-ui/core/CssBaseline';
import Typography from '@material-ui/core/Typography';
import AddIcon from '@material-ui/icons/Add';
import Divider from '@material-ui/core/Divider';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import Zoom from '@material-ui/core/Zoom';
import Fab from '@material-ui/core/Fab';
import {useQuery} from 'urql';
import AddClusterDialog from './AddClusterDialog'
import Container from '@material-ui/core/Container';
import Grid from '@material-ui/core/Grid';
import Paper from '@material-ui/core/Paper';
import {
    Link,
} from "react-router-dom";

const ClustersQuery = `
  query {
    clusters {
      id
      name
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
        height: '100vh',
        overflow: 'auto',
    },
    fab: {
        position: 'absolute',
        bottom: theme.spacing(2),
        right: theme.spacing(2),
    },
    container: {
        paddingTop: theme.spacing(4),
        paddingBottom: theme.spacing(4),
    },
    paper: {
        padding: theme.spacing(2),
        display: 'flex',
        overflow: 'auto',
        flexDirection: 'column',
    },
    fixedHeight: {
        height: 500,
    },
}));

export default function Clusters() {
    const classes = useStyles();
    const theme = useTheme();
    const [dialogOpen, setDialogOpen] = React.useState(false);
    const [currentCluster, setCurrentCluster] = React.useState(null);

    const [result, reexecuteQuery] = useQuery({
        query: ClustersQuery,
    });

    const fixedHeightPaper = clsx(classes.paper, classes.fixedHeight);

    const transitionDuration = {
        enter: theme.transitions.duration.enteringScreen,
        exit: theme.transitions.duration.leavingScreen,
    };

    const {data, fetching, error} = result;

    if (fetching) return <p>Loading...</p>;
    if (error) return <p>Oh no... {error.message}</p>;

    const handleDialogOnClose = () => {
        reexecuteQuery({requestPolicy: 'network-only'});
        setDialogOpen(false);
    }


    return (
        <div className={classes.root}>
            <CssBaseline/>
            <AppBar
                position="fixed"
                className={clsx(classes.appBar)}
            >
                <Toolbar>
                    <Typography variant="h6" noWrap>
                        Clusters
                    </Typography>
                </Toolbar>
            </AppBar>
            <main className={classes.content}>
                <div className={classes.toolbar}/>
                <Container maxWidth="lg" className={classes.container}>
                    <Grid container spacing={3}>
                        <Grid item xs={12} md={10} lg={12}>
                            <Paper className={fixedHeightPaper}>
                                <List>
                                    {data.clusters.map(cluster => (

                                        <Link to={"/cluster/" + cluster.id}>
                                            <ListItem button key={cluster.id}>
                                                <ListItemText primary={cluster.name}>
                                                </ListItemText>
                                            </ListItem>
                                            <Divider/>
                                        </Link>
                                    ))}
                                </List>
                            </Paper>
                        </Grid>
                    </Grid>
                </Container>
            </main>
            <Zoom
                key='primary'
                in='true'
                timeout={transitionDuration}
                unmountOnExit
            >
                <Fab aria-label='Add' className={classes.fab} color='primary' onClick={() => {
                    console.log("click")
                    setDialogOpen(true)
                }}>
                    <AddIcon/>
                </Fab>
            </Zoom>
            <AddClusterDialog open={dialogOpen} onClose={handleDialogOnClose}/>
        </div>
    );
}

