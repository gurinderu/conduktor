import React, {useRef} from 'react';
import {makeStyles} from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import CloseIcon from '@material-ui/icons/Close';
import Slide from '@material-ui/core/Slide';
import {Paper} from "@material-ui/core";
import {Grid} from "@material-ui/core";
import {TextField} from "@material-ui/core";
import {DataGrid} from '@material-ui/data-grid';
import clsx from "clsx";
import AddIcon from "@material-ui/icons/Add";
import DeleteIcon from "@material-ui/icons/Delete";
import {useMutation} from 'urql';

const useStyles = makeStyles((theme) => ({
    appBar: {
        position: 'relative',
    },
    title: {
        marginLeft: theme.spacing(2),
        flex: 1,
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

const Transition = React.forwardRef(function Transition(props, ref) {
    return <Slide direction="up" ref={ref} {...props} />;
});

const AddCluster = `
 mutation ($name:String!,$servers:String!, $props: [PropertyInput!]){
  cluster{
    add(name:$name, bootstrapServers:$servers, properties:$props){
      id
    }
  }
}
`;

export default function AddClusterDialog({open, onClose}) {
    const classes = useStyles();
    const [_open, _setOpen] = React.useState(open);
    const [rows, setRows] = React.useState([]);
    const [selectedRows, setSelectedRows] = React.useState([]);
    const [addClusterResult, addCluster] = useMutation(AddCluster);

    const columns = [
        {field: 'id', headerName: 'ID', hide: true},
        {
            field: 'key',
            headerName: 'Key',
            minWidth: 300,
            editable: true,
            flex: 1,
            resizable: false
        },
        {
            field: 'value',
            headerName: 'Value',
            minWidth: 300,
            editable: true,
            flex: 1,
            resizable: false
        }
    ];

    const handleClose = () => {
        _setOpen(false);
        onClose();
    };
    const handleSave = () => {
        const variables = {
            name: nameRef.current.value,
            servers: bootstrapRef.current.value,
            props: rows.length === 0 ? null : rows
        };

        addCluster(variables).then(result => {
                _setOpen(false);
                onClose();
            }
        )


    };
    const fixedHeightPaper = clsx(classes.paper, classes.fixedHeight);
    const handleAdd = () => {
        setRows([...rows, {
            id: rows.length + 1,
            key: "",
            value: ""
        }])
    }
    const handleDelete = () => {
        const updatedRows = rows.filter((row) => {
            let selected = selectedRows.some(e => e === row.id)
            return !selected;
        });
        setRows(updatedRows);
    }

    const handleCellEditCommit = ({id, field, value}) => {
        const updatedRows = rows.map((row) => {
            if (row.id === id) {
                let newRow = {...row};
                newRow[field] = value;
                return newRow;
            }
            return row;
        });
        setRows(updatedRows);
    }

    const handleSelectionModelChange = (model) => {
        setSelectedRows(model)
    }

    const nameRef = useRef('')
    const bootstrapRef = useRef('')

    return (
        <Dialog fullScreen open={open} onClose={handleClose} TransitionComponent={Transition}>
            <AppBar className={classes.appBar}>
                <Toolbar>
                    <IconButton edge="start" color="inherit" onClick={handleClose} aria-label="close">
                        <CloseIcon/>
                    </IconButton>
                    <Typography variant="h6" className={classes.title}>
                        Add cluster
                    </Typography>
                    <Button autoFocus color="inherit" onClick={handleSave}>
                        save
                    </Button>
                </Toolbar>
            </AppBar>
            <main className={classes.layout}>
                <Grid container spacing={3}>
                    <Grid item xs={12} md={10} lg={12}>
                        <Paper className={fixedHeightPaper}>
                            <Grid container spacing={5}>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        required
                                        id="name"
                                        name="name"
                                        label="Cluster name"
                                        inputRef={nameRef}
                                        fullWidth
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        required
                                        id="bootstap"
                                        name="bootstap"
                                        label="Bootstrap address"
                                        inputRef={bootstrapRef}
                                        fullWidth/>
                                </Grid>
                                <Grid item xs={24} sm={12}>
                                    <Toolbar>
                                        <Typography variant="h6" className={classes.title}>
                                            Properties
                                        </Typography>
                                        <IconButton edge="start" color="inherit" onClick={handleAdd} aria-label="close">
                                            <AddIcon/>
                                        </IconButton>
                                        <IconButton edge="start" color="inherit" onClick={handleDelete}
                                                    aria-label="close">
                                            <DeleteIcon/>
                                        </IconButton>
                                    </Toolbar>
                                    <DataGrid
                                        columns={columns}
                                        rows={rows}
                                        autoHeight={true}
                                        hideFooterPagination={true}
                                        checkboxSelection={true}
                                        onCellEditCommit={handleCellEditCommit}
                                        onSelectionModelChange={handleSelectionModelChange}
                                    />
                                </Grid>
                            </Grid>
                        </Paper>
                    </Grid></Grid>
            </main>
        </Dialog>);
}
